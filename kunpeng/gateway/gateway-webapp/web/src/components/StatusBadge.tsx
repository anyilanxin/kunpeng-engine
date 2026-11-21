import { Tag } from 'antd';

const STATE_COLOR: Record<string, string> = {
  SUCCEED: 'success',
  COMPLETED: 'success',
  EXECUTED: 'processing',
  EXECUTING: 'processing',
  WAIT: 'default',
  FAILED: 'error',
  PLAN_CREATED: 'cyan',
  PLAN_DELAYED: 'warning',
  PLAN_CREATING: 'cyan',
  COMPLETING: 'cyan',
};

/** 调度计划/执行明细状态标签 */
export function StateTag({ state }: { state: string }) {
  return <Tag color={STATE_COLOR[state] ?? 'default'}>{state}</Tag>;
}
