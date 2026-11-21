import { useState } from 'react';
import { Alert, App, Button, Form, Input } from 'antd';
import { ArrowRightOutlined, LockOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons';
import { useLocation, useNavigate } from 'react-router-dom';
import { login } from '../api';
import ClusterBrand from '../components/ClusterBrand';
import LoginClusterVisual from '../components/LoginClusterVisual';

interface LoginFormValues {
  username: string;
  password: string;
}

export default function LoginPage({ onLoggedIn }: { onLoggedIn: (user: { username: string; roles: string[] }) => void }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const location = useLocation();
  const { message } = App.useApp();

  const onFinish = async ({ username, password }: LoginFormValues) => {
    setLoading(true);
    setError('');
    try {
      const user = await login(username.trim(), password);
      onLoggedIn(user);
      message.success(`登录成功，欢迎 ${user.username}`);
      const from = (location.state as { from?: string } | null)?.from ?? '/';
      navigate(from, { replace: true });
    } catch (e) {
      setError(e instanceof Error ? e.message : '登录失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <div className="login-page__ambient login-page__ambient--one" />
      <div className="login-page__ambient login-page__ambient--two" />
      <section className="login-shell">
        <div className="login-showcase">
          <div className="login-showcase__brand"><ClusterBrand /></div>
          <div className="login-showcase__content">
            <div className="login-showcase__eyebrow"><span /> DISTRIBUTED WORKFLOW ENGINE</div>
            <h1>洞察集群状态<br />守护每一次任务流转</h1>
            <p>面向微服务编排引擎的一体化集群控制台，<br />让节点、分区与 JobWorker 管理更简单。</p>
          </div>
          <LoginClusterVisual />
          <div className="login-showcase__metrics">
            <div><strong>BPMN</strong><span><i /> 流程编排</span></div>
            <div><strong>Raft</strong><span>分区日志复制</span></div>
            <div><strong>gRPC</strong><span>任务推拉</span></div>
          </div>
          <div className="login-showcase__grid" />
        </div>

        <div className="login-form-panel">
          <div className="login-form-panel__mobile-brand"><ClusterBrand /></div>
          <div className="login-form-wrap">
            <div className="login-form-heading">
              <span className="login-form-heading__icon"><SafetyCertificateOutlined /></span>
              <div className="login-form-heading__eyebrow">WELCOME BACK</div>
              <h2>登录集群控制台</h2>
              <p>使用管理员账号继续访问 Kunpeng Engine 集群</p>
            </div>
            {error && <Alert className="login-error" type="error" showIcon message={error} />}
            <Form<LoginFormValues>
              layout="vertical"
              requiredMark={false}
              onFinish={onFinish}
              className="login-form"
            >
              <Form.Item label="账号" name="username" rules={[{ required: true, whitespace: true, message: '请输入管理员账号' }]}>
                <Input size="large" prefix={<UserOutlined />} placeholder="请输入账号" autoComplete="username" allowClear />
              </Form.Item>
              <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入登录密码' }, { min: 6, message: '密码至少为 6 位' }]}>
                <Input.Password size="large" prefix={<LockOutlined />} placeholder="请输入密码" autoComplete="current-password" />
              </Form.Item>
              <div className="login-form-options login-form-options--end">
                <Button type="link" onClick={() => message.info('请联系集群管理员重置密码')}>忘记密码？</Button>
              </div>
              <Button className="login-submit" type="primary" htmlType="submit" loading={loading} block>
                {loading ? '正在验证...' : '登录'} {!loading && <ArrowRightOutlined />}
              </Button>
            </Form>
            <p className="login-session-note">登录将建立服务端 Web 会话，账号由集群配置文件统一管理。</p>
          </div>
          <footer className="login-form-footer"><span>KUNPENG CLUSTER CONSOLE</span><small>安全连接 · Session Cookie</small></footer>
        </div>
      </section>
      <div className="login-page__footer">© 2026 Kunpeng Engine</div>
    </main>
  );
}
