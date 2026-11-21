import { Alert } from 'antd';

export function ErrorAlert({ message }: { message: string }) {
  return <Alert type="error" showIcon message={message} />;
}
