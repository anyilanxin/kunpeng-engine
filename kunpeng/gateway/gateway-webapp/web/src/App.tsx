import { useEffect, useState } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { Spin } from 'antd';
import { useTranslation } from 'react-i18next';
import { me } from './api';
import type { MeInfo } from './api';
import AdminLayout from './layouts/AdminLayout';
import DashboardPage from './pages/DashboardPage';
import NodesPage from './pages/NodesPage';
import PartitionsPage from './pages/PartitionsPage';
import AdminPartitionsPage from './pages/AdminPartitionsPage';
import JobWorkersPage from './pages/JobWorkersPage';
import DispatchPage from './pages/DispatchPage';
import GatewaysPage from './pages/GatewaysPage';
import LoginPage from './pages/LoginPage';
import { ClusterDataProvider } from './data/ClusterDataContext';

export interface CurrentUser extends MeInfo {}

export default function App() {
  const { t } = useTranslation();
  const [user, setUser] = useState<CurrentUser>();
  const [checking, setChecking] = useState(true);
  const location = useLocation();

  useEffect(() => {
    let active = true;
    me()
      .then((current) => { if (active) setUser(current); })
      .catch(() => { if (active) setUser(undefined); })
      .finally(() => { if (active) setChecking(false); });
    return () => { active = false; };
  }, [location.pathname]);

  if (checking) {
    return (
      <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', background: 'var(--bg)' }}>
        <Spin size="large" tip={t('app.common.loading')}><div style={{ width: 120, height: 60 }} /></Spin>
      </div>
    );
  }

  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage onLoggedIn={setUser} />} />
        <Route path="*" element={<LoginPage onLoggedIn={setUser} />} />
      </Routes>
    );
  }

  return (
    <ClusterDataProvider>
      <Routes>
        <Route path="/login" element={<Navigate to="/" replace />} />
        <Route element={<AdminLayout user={user} onLoggedOut={() => setUser(undefined)} />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/nodes" element={<NodesPage />} />
          <Route path="/partitions" element={<PartitionsPage />} />
          <Route path="/admin-partitions" element={<AdminPartitionsPage />} />
          <Route path="/workers" element={<JobWorkersPage />} />
          <Route path="/dispatch" element={<DispatchPage />} />
          <Route path="/gateways" element={<GatewaysPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </ClusterDataProvider>
  );
}
