/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package structpack

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task that scans Java source files annotated with
 * {@code @AutoDeclareProperties} and ensures their no-arg constructor calls
 * {@code super(N)} and chains {@code declareProperty(...)} for every Property field.
 *
 * <p>This task rewrites the Java source files directly.  Field initializers are left
 * untouched; only the no-arg constructor body is generated or corrected.
 *
 * <p>structpack 模式（类 import 含 {@code com.anyilanxin.kunpeng.structpack}）额外执行
 * id 身份管理：id 直接生成进源码 —— 字段初始化器 {@code new XxxProperty(id, key, ...)}
 * 首参，与 key/类型/默认值同在一行。无源码内 id 账本, 只看当前字段状态：
 *
 * <ul>
 *   <li>新字段（初始化器无 id）→ 取 [1,127] 内最小空闲 id（空洞先回填, 紧凑时即 max+1
 *       递增——两端序号都用起来, 避免一味增大越过 127）
 *   <li>重复 id / id &lt; 1（默认最小 1）→ 按声明顺序保留首个合法值, 其余同样取最小空闲重排
 *   <li>id &gt; 127（1 字节保证）/ 可用 id 耗尽 → 构建失败
 * </ul>
 *
 * <p>历史构建生成的 {@code structpack-ids} 标记注释已废弃, 任务遇到时整块移除。
 */
class AutoDeclarePropertiesTask extends DefaultTask {

  @InputFiles
  @Optional
  Object sourceFiles

  @OutputFiles
  @Optional
  FileCollection getOutputFiles() {
    return sourceFiles == null ? null : project.files(sourceFiles)
  }

  @TaskAction
  void process() {
    if (sourceFiles == null) {
      return
    }

    final Collection<File> files = project.files(sourceFiles).files
    if (files.isEmpty()) {
      return
    }

    final ParserConfiguration config = new ParserConfiguration()
    config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
    final JavaParser parser = new JavaParser(config)
    files.each { file ->
      if (!file.name.endsWith('.java')) {
        return
      }
      processFile(parser, file)
    }
  }

  void processFile(final JavaParser parser, final File file) {
    final parseResult = parser.parse(file)
    if (!parseResult.successful) {
      return
    }

    final CompilationUnit cu = parseResult.result.orElse(null)
    if (cu == null) {
      return
    }

    // Preserve formatting as much as possible while we mutate the AST.
    LexicalPreservingPrinter.setup(cu)

    final boolean structPackMode = usesStructPack(cu)
    boolean modified = false
    cu.findAll(ClassOrInterfaceDeclaration).each { typeDecl ->
      if (hasAnnotation(typeDecl, 'AutoDeclareProperties')) {
        if (structPackMode) {
          if (processTypeIdMode(typeDecl)) {
            modified = true
          }
        } else if (processType(typeDecl)) {
          modified = true
        }
      }
    }

    if (modified) {
      String printed = LexicalPreservingPrinter.print(cu)
      printed = formatDeclarePropertyChains(printed)
      if (!printed.endsWith('\n')) {
        printed += '\n'
      }
      file.text = printed
      logger.lifecycle("Auto-declared properties in ${file.path}")
    }
    if (structPackMode) {
      stripIdMarkers(file)
    }
  }

  /**
   * 移除历史构建生成的 structpack-ids 标记块（id 账本机制已废弃）。
   * 整块匹配: 名称行 + 连续纯 id 续行, 连同行尾换行一并删除。
   */
  static void stripIdMarkers(final File file) {
    final String text = file.text
    if (!text.contains(MARKER_PREFIX)) {
      return
    }
    final String stripped = text.replaceAll(
        "(?m)^[ \\t]*\\/\\/\\s*structpack-ids\\[[^\\]\\n]*\\]:[^\\n]*" +
        "(?:\\r?\\n[ \\t]*\\/\\/[ \\t0-9,\\r]+(?![^\\n]))*(?:\\r?\\n)?", '')
    if (stripped != text) {
      file.text = stripped
    }
  }

  static String formatDeclarePropertyChains(final String text) {
    return text.split('\n').collect { line ->
      if (line.contains('.declareProperty(') && line.trim().startsWith('declareProperty(')) {
        final String indent = line.takeWhile { it == ' ' || it == '\t' }
        final String trimmed = line.trim().replaceAll(';$', '')
        final String[] calls = trimmed.split('\\.declareProperty\\(')
        final String first = calls[0]
        final List<String> rest =
            calls[1..-1].collect { ".declareProperty(${it}" }
        final List<String> formatted = [indent + '// formatting:off']
        formatted.add(indent + first)
        for (int i = 0; i < rest.size() - 1; i++) {
          formatted.add(indent + '    ' + rest[i])
        }
        formatted.add(indent + '    ' + rest.last() + ';')
        formatted.add(indent + '// formatting:on')
        return formatted.join('\n')
      }
      line
    }.join('\n')
  }

  static boolean hasAnnotation(
      final ClassOrInterfaceDeclaration typeDecl, final String simpleName) {
    typeDecl.annotations.any { it.nameAsString == simpleName }
  }

  /**
   * Returns {@code true} if the type declaration was modified.
   */
  static boolean processType(final ClassOrInterfaceDeclaration typeDecl) {
    final List<FieldDeclaration> propertyFields = collectPropertyFields(typeDecl)
    if (propertyFields.isEmpty()) {
      return false
    }

    final ConstructorDeclaration noArgConstructor = findNoArgConstructor(typeDecl)
    if (noArgConstructor != null) {
      return fixConstructor(noArgConstructor, propertyFields)
    }

    return addConstructor(typeDecl, propertyFields)
  }

  static List<FieldDeclaration> collectPropertyFields(
      final ClassOrInterfaceDeclaration typeDecl) {
    final List<FieldDeclaration> propertyFields = []
    typeDecl.fields.each { fieldDecl ->
      if (fieldDecl.variables.empty) {
        return
      }
      if (isPropertyType(fieldDecl.commonType)) {
        propertyFields.add(fieldDecl)
      }
    }
    return propertyFields
  }

  static boolean isPropertyType(final Type type) {
    if (!(type instanceof ClassOrInterfaceType)) {
      return false
    }
    final String name = ((ClassOrInterfaceType) type).nameAsString
    return name.endsWith('Property') || name == 'BaseProperty'
  }

  static ConstructorDeclaration findNoArgConstructor(
      final ClassOrInterfaceDeclaration typeDecl) {
    // Only return an explicit no-arg constructor.  The default constructor returned by
    // typeDecl.defaultConstructor is synthetic and cannot be modified in the source output.
    return typeDecl.constructors.find { it.parameters.empty }
  }

  static boolean fixConstructor(
      final ConstructorDeclaration constructor, final List<FieldDeclaration> propertyFields) {
    final BlockStmt body = constructor.body
    final int capacity = propertyFields.size()

    // If the existing constructor already has the correct super(capacity) and the
    // correct multi-line declareProperty chain, leave it alone.  The formatting
    // markers are generated by formatDeclarePropertyChains and are honored by
    // Spotless, but they are not visible in LexicalPreservingPrinter output.
    final ExplicitConstructorInvocationStmt superCall = findSuperCall(body)
    if (superCall != null
        && superCall.arguments.size() == 1
        && isIntegerLiteral(superCall.arguments[0], capacity)
        && hasCorrectDeclarePropertyChain(body, propertyFields)) {
      return false
    }

    // Otherwise replace the whole constructor body with a correct one.
    constructor.setBody(buildConstructorBody(capacity, propertyFields))
    return true
  }

  static boolean addConstructor(
      final ClassOrInterfaceDeclaration typeDecl,
      final List<FieldDeclaration> propertyFields) {
    final ConstructorDeclaration constructor = new ConstructorDeclaration()
    constructor.setModifiers(NodeList.nodeList(Modifier.publicModifier()))
    constructor.setName(typeDecl.name)
    constructor.setParameters(new NodeList<>())
    constructor.setBody(buildConstructorBody(propertyFields.size(), propertyFields))

    // Insert the constructor right after the last field declaration so it appears
    // before other methods, matching typical Java class layout.
    int insertIndex = 0
    final List<BodyDeclaration<?>> members = typeDecl.members
    for (int i = 0; i < members.size(); i++) {
      if (members.get(i) instanceof FieldDeclaration) {
        insertIndex = i + 1
      }
    }
    members.add(insertIndex, constructor)
    return true
  }

  static BlockStmt buildConstructorBody(
      final int capacity, final List<FieldDeclaration> propertyFields) {
    final BlockStmt body = new BlockStmt()
    body.addStatement(
        new ExplicitConstructorInvocationStmt(
            false, null, NodeList.nodeList(new IntegerLiteralExpr(capacity))))

    if (!propertyFields.isEmpty()) {
      MethodCallExpr chain = null
      propertyFields.each { fieldDecl ->
        final String fieldName = fieldDecl.variables[0].nameAsString
        final MethodCallExpr call =
            new MethodCallExpr(
                null, 'declareProperty', NodeList.nodeList(new NameExpr(fieldName)))
        if (chain == null) {
          chain = call
        } else {
          call.setScope(chain)
          chain = call
        }
      }
      body.addStatement(new ExpressionStmt(chain))
    }

    return body
  }

  static ExplicitConstructorInvocationStmt findSuperCall(final BlockStmt body) {
    for (final Statement stmt : body.statements) {
      if (stmt instanceof ExplicitConstructorInvocationStmt && !stmt.this) {
        return (ExplicitConstructorInvocationStmt) stmt
      }
    }
    return null
  }

  static boolean isIntegerLiteral(
      final com.github.javaparser.ast.expr.Expression expr, final int expectedValue) {
    return expr instanceof IntegerLiteralExpr
        && Integer.parseInt(((IntegerLiteralExpr) expr).value) == expectedValue
  }

  static boolean hasCorrectDeclarePropertyChain(
      final BlockStmt body, final List<FieldDeclaration> propertyFields) {
    final List<String> expectedNames =
        propertyFields.collect { it.variables[0].nameAsString }

    // Require exactly one expression statement (the chained declareProperty call).
    final List<ExpressionStmt> exprStmts =
        body.statements.findAll {
          it instanceof ExpressionStmt && it.expression instanceof MethodCallExpr
        }
    if (exprStmts.size() != 1) {
      return false
    }

    final List<String> actualNames = []
    final ExpressionStmt chainStmt = (ExpressionStmt) exprStmts[0]
    collectDeclarePropertyNames((MethodCallExpr) chainStmt.expression, actualNames)

    // For multi-property chains require the statement to span multiple lines so
    // each .declareProperty(...) call is on its own line.  A single-property
    // chain is valid as a one-line statement.
    if (expectedNames.size() > 1
        && chainStmt.begin.present
        && chainStmt.end.present
        && chainStmt.begin.get().line == chainStmt.end.get().line) {
      return false
    }

    return actualNames == expectedNames
  }

  static void collectDeclarePropertyNames(
      final MethodCallExpr call, final List<String> names) {
    // Handle chained calls: this.declareProperty(a).declareProperty(b)
    if (call.scope.present && call.scope.get() instanceof MethodCallExpr) {
      collectDeclarePropertyNames((MethodCallExpr) call.scope.get(), names)
    }
    if (call.nameAsString == 'declareProperty' && call.arguments.size() == 1) {
      final arg = call.arguments[0]
      if (arg instanceof NameExpr) {
        names.add(((NameExpr) arg).nameAsString)
      }
    }
  }


  // ===== structpack id 模式: id 生成进源码初始化器 =====

  static boolean usesStructPack(final CompilationUnit cu) {
    cu.imports.any { it.nameAsString.startsWith('com.anyilanxin.kunpeng.structpack') }
  }

  static final String MARKER_PREFIX = 'structpack-ids'

  /** 表达式整型字面量值（含一元负号）; 非整型字面量返回 null。 */
  static Integer literalInt(final Expression expr) {
    if (expr instanceof IntegerLiteralExpr) {
      return Integer.parseInt(((IntegerLiteralExpr) expr).value)
    }
    if (expr instanceof UnaryExpr
        && ((UnaryExpr) expr).operator == UnaryExpr.Operator.MINUS
        && ((UnaryExpr) expr).expression instanceof IntegerLiteralExpr) {
      return -Integer.parseInt(((IntegerLiteralExpr) ((UnaryExpr) expr).expression).value)
    }
    return null
  }

  /**
   * structpack id 身份管理：向初始化器插入/校验 id, 生成构造器链（保留 declareGhost）。
   *
   * <p>无源码内账本, 只看当前字段状态: id 默认最小 1, 分配取 [1,127] 内最小空闲 id
   * （空洞先回填, 紧凑时即 max+1 递增）; 重复或 &lt;1 的 id 按声明顺序保留首个合法值,
   * 其余摘除后同新字段一起重排。
   *
   * @return {@code true} 表示源码被修改（调用方负责写回）
   */
  boolean processTypeIdMode(final ClassOrInterfaceDeclaration typeDecl) {
    final List<FieldDeclaration> propertyFields = collectPropertyFields(typeDecl)
    if (propertyFields.isEmpty()) {
      return false
    }

    // 1. 收集现有 id; id 默认最小 1 —— 重复或 <1 一律摘除首参转为待分配重排
    boolean modified = false
    final Set<Integer> usedIds = new TreeSet<>()
    final List<ObjectCreationExpr> unassigned = []
    propertyFields.each { fieldDecl ->
      final def initializer = fieldDecl.variables.empty
          ? null
          : fieldDecl.variables[0].initializer.orElse(null)
      if (!(initializer instanceof ObjectCreationExpr)) {
        return
      }
      final def args = ((ObjectCreationExpr) initializer).arguments
      final Integer id = args.isEmpty() ? null : literalInt(args[0])
      if (id != null) {
        if (id >= 1 && !usedIds.contains(id)) {
          usedIds.add(id)
        } else {
          ((ObjectCreationExpr) initializer).arguments.remove(0)
          unassigned.add(initializer)
          modified = true
          logger.lifecycle("${typeDecl.nameAsString}: ${id < 1 ? '非法' : '重复'} id=${id} 待重排")
        }
      } else if (!args.isEmpty() && (args[0] instanceof StringLiteralExpr
          || args[0] instanceof NameExpr)) {
        // 首参为 key（字符串字面量或静态导入常量）→ 无 id, 待分配
        unassigned.add(initializer)
      }
    }

    // 2. id 上界守卫: 127（1 字节保证; 下界 1 由收集阶段保证, 非法值已转待重排）
    usedIds.each { id ->
      if (id > 127) {
        throw new GradleException(
            "${typeDecl.nameAsString}: id ${id} 超上界（必须 ≤127, 每字段 1 字节保证）—— 请手写修正")
      }
    }

    // 3. 分配: 取 [1,127] 内最小空闲 id —— 空洞先回填（低位空洞/删除遗留）,
    //    紧凑无洞时即 max+1 递增, 两端序号都用起来, 避免一味增大越过 127
    unassigned.each { creation ->
      int candidate = 1
      while (candidate <= 127 && usedIds.contains(candidate)) {
        candidate++
      }
      if (candidate > 127) {
        throw new GradleException(
            "${typeDecl.nameAsString}: 可用 id 耗尽（>127, 1 字节保证）—— 请整理现有 id")
      }
      usedIds.add(candidate)
      ((ObjectCreationExpr) creation).arguments.addFirst(new IntegerLiteralExpr(candidate))
      modified = true
      logger.lifecycle("${typeDecl.nameAsString}: 分配 id=${candidate}")
    }

    // 4. 构造器链
    return processType(typeDecl) || modified
  }

}
