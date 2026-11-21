import type { CSSProperties } from 'react';

interface PartitionVisualProps {
  /** 分区（RAFT 组）数量 */
  columns?: number;
  /** 各列 Leader 所在行（0=n01 1=n02 2=n03），缺省轮询分布 */
  leaderRows?: number[];
  /** 异常副本位置 */
  warn?: { column: number; row: number };
  badge?: string;
  /** 单分区布局（收窄容器） */
  single?: boolean;
}

interface ColumnSpec {
  leader: number;
  delay: string;
  warn?: number;
}

/* Leader/Follower 矩阵动画：日志复制点沿列从 Leader 流向 Follower 副本 */
export default function PartitionVisual({
  columns = 6,
  leaderRows,
  warn,
  badge = '多 RAFT 组 · 日志复制中',
  single = false,
}: PartitionVisualProps) {
  const cycle = leaderRows ?? Array.from({ length: columns }, (_, index) => index % 3);
  const specs: ColumnSpec[] = cycle.map((leader, index) => ({
    leader,
    delay: `${(index * 0.55) % 1.7}s`,
    warn: warn && warn.column === index ? warn.row : undefined,
  }));

  return (
    <div className={`partition-matrix-visual${single ? ' partition-matrix-visual--single' : ''}`} aria-hidden="true">
      <span className="pmv-badge"><i className="live-dot" />{badge}</span>
      <div className="pmv-labels"><span>n01</span><span>n02</span><span>n03</span></div>
      <div className="pmv-cols">
        {specs.map((col, index) => (
          <div className="pmv-col" key={index} style={{ '--d': col.delay, animationDelay: `${index * 80}ms` } as CSSProperties}>
            <i className="pmv-stream" />
            <span className="pmv-dot" />
            <span className="pmv-dot pmv-dot--late" />
            {[0, 1, 2].map((row) => {
              const isLeader = col.leader === row;
              const isWarn = col.warn === row && !isLeader;
              return (
                <b
                  key={row}
                  className={`pmv-cell ${isLeader ? 'pmv-cell--leader' : 'pmv-cell--follower'}${isWarn ? ' pmv-cell--warn' : ''}`}
                >
                  {isLeader ? 'Leader' : 'Follower'}
                  {isWarn && <i />}
                </b>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
}
