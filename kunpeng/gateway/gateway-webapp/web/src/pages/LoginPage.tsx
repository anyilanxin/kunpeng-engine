import { useState } from 'react';
import { Alert, App, Button, Form, Input } from 'antd';
import { ArrowRightOutlined, LockOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useLocation, useNavigate } from 'react-router-dom';
import { login } from '../api';
import type { MeInfo } from '../api';
import ClusterBrand from '../components/ClusterBrand';
import { PreferenceSwitchers } from '../components/PreferenceSwitchers';
import LoginClusterVisual from '../components/LoginClusterVisual';

interface LoginFormValues {
  username: string;
  password: string;
}

export default function LoginPage({ onLoggedIn }: { onLoggedIn: (user: MeInfo) => void }) {
  const { t } = useTranslation();
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
      message.success(t('app.common.welcome', { name: user.username }));
      const from = (location.state as { from?: string } | null)?.from ?? '/';
      navigate(from, { replace: true });
    } catch (e) {
      setError(e instanceof Error ? e.message : t('app.common.loginFailed'));
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
            <h1>{t('app.login.heroTitle1')}<br />{t('app.login.heroTitle2')}</h1>
            <p>{t('app.login.heroDesc1')}<br />{t('app.login.heroDesc2')}</p>
          </div>
          <LoginClusterVisual />
          <div className="login-showcase__metrics">
            <div><strong>BPMN</strong><span><i /> {t('app.login.metricOrchestration')}</span></div>
            <div><strong>Raft</strong><span>{t('app.login.metricReplication')}</span></div>
            <div><strong>gRPC</strong><span>{t('app.login.metricPushPull')}</span></div>
          </div>
          <div className="login-showcase__grid" />
        </div>

        <div className="login-form-panel">
          <div className="login-switchers"><PreferenceSwitchers /></div>
          <div className="login-form-panel__mobile-brand"><ClusterBrand /></div>
          <div className="login-form-wrap">
            <div className="login-form-heading">
              <span className="login-form-heading__icon"><SafetyCertificateOutlined /></span>
              <div className="login-form-heading__eyebrow">WELCOME BACK</div>
              <h2>{t('app.login.heading')}</h2>
              <p>{t('app.login.subheading')}</p>
            </div>
            {error && <Alert className="login-error" type="error" showIcon message={error} />}
            <Form<LoginFormValues>
              layout="vertical"
              requiredMark={false}
              onFinish={onFinish}
              className="login-form"
            >
              <Form.Item label={t('app.login.username')} name="username" rules={[{ required: true, whitespace: true, message: t('app.login.usernameRequired') }]}>
                <Input size="large" prefix={<UserOutlined />} placeholder={t('app.login.usernamePlaceholder')} autoComplete="username" allowClear />
              </Form.Item>
              <Form.Item label={t('app.login.password')} name="password" rules={[{ required: true, message: t('app.login.passwordRequired') }, { min: 6, message: t('app.login.passwordMin') }]}>
                <Input.Password size="large" prefix={<LockOutlined />} placeholder={t('app.login.passwordPlaceholder')} autoComplete="current-password" />
              </Form.Item>
              <div className="login-form-options login-form-options--end">
                <Button type="link" onClick={() => message.info(t('app.common.resetPassword'))}>{t('app.login.forgot')}</Button>
              </div>
              <Button className="login-submit" type="primary" htmlType="submit" loading={loading} block>
                {loading ? t('app.login.submitting') : t('app.login.submit')} {!loading && <ArrowRightOutlined />}
              </Button>
            </Form>
            <p className="login-session-note">{t('app.login.sessionNote')}</p>
          </div>
          <footer className="login-form-footer"><span>KUNPENG CLUSTER CONSOLE</span><small>{t('app.login.footerSecurity')}</small></footer>
        </div>
      </section>
      <div className="login-page__footer">© 2026 Kunpeng Engine</div>
    </main>
  );
}
