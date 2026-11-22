import { useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { AutoComplete, Avatar, Badge, Button, Drawer, Dropdown, Input, Layout, Menu } from 'antd';
import type { InputRef, MenuProps } from 'antd';
import { useTranslation } from 'react-i18next';
import {
  ApiOutlined,
  BellOutlined,
  CheckCircleFilled,
  ClusterOutlined,
  ControlOutlined,
  DashboardOutlined,
  DownOutlined,
  GatewayOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuOutlined,
  MenuUnfoldOutlined,
  PartitionOutlined,
  SearchOutlined,
  SwapOutlined,
  WarningFilled,
} from '@ant-design/icons';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import ClusterBrand from '../components/ClusterBrand';
import { LanguageToggle, ThemeToggle } from '../components/PreferenceSwitchers';
import { useBrokers } from '../data/ClusterDataContext';
import { logout } from '../api';
import type { MeInfo } from '../api';
import { getPartitionCells } from '../utils/cluster';

const { Header, Sider, Content } = Layout;

/** 菜单路由表：label 走 i18n（语言切换即时生效），subtitle 维持英文副标（现状设计） */
const ROUTES: { path: string; labelKey: string; subtitle: string }[] = [
  { path: '/', labelKey: 'app.menu.dashboard', subtitle: 'Cluster overview' },
  { path: '/nodes', labelKey: 'app.menu.nodes', subtitle: 'Broker nodes' },
  { path: '/partitions', labelKey: 'app.menu.partitions', subtitle: 'Business raft partitions' },
  { path: '/admin-partitions', labelKey: 'app.menu.adminPartitions', subtitle: 'Admin partitions' },
  { path: '/workers', labelKey: 'app.menu.workers', subtitle: 'Job orchestration' },
  { path: '/dispatch', labelKey: 'app.menu.dispatch', subtitle: 'Dispatch plans & cluster meta' },
  { path: '/gateways', labelKey: 'app.menu.gateways', subtitle: 'Gateway discovery & load' },
];

const MENU_ICONS: Record<string, ReactNode> = {
  '/': <DashboardOutlined />,
  '/nodes': <ClusterOutlined />,
  '/partitions': <PartitionOutlined />,
  '/admin-partitions': <ControlOutlined />,
  '/workers': <ApiOutlined />,
  '/dispatch': <SwapOutlined />,
  '/gateways': <GatewayOutlined />,
};

export default function AdminLayout({ user, onLoggedOut }: { user: MeInfo; onLoggedOut: () => void }) {
  const { t } = useTranslation();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [search, setSearch] = useState('');
  const inputRef = useRef<InputRef>(null);
  const location = useLocation();
  const navigate = useNavigate();
  const brokers = useBrokers();
  const partitions = useMemo(() => getPartitionCells(brokers), [brokers]);
  const logicalPartitions = new Set(partitions.map((item) => `${item.group}-${item.id}`)).size;
  const unhealthy = partitions.filter((item) => item.health !== 'HEALTHY');

  const currentRoute = useMemo(() => {
    const route = ROUTES.find((item) => item.path === location.pathname) ?? ROUTES[0];
    return { title: t(route.labelKey), subtitle: route.subtitle };
  }, [location.pathname, t]);

  const menuItems = useMemo<NonNullable<MenuProps['items']>>(() => ROUTES.map((route) => ({
    key: route.path,
    icon: MENU_ICONS[route.path],
    label: t(route.labelKey),
  })), [t]);

  /** 页面搜索选项：label 用当前语言，导航查表随语言重建 */
  const searchRoutes = useMemo(() => Object.fromEntries(ROUTES.map((route) => [t(route.labelKey), route.path])), [t]);
  const pageSuffix = t('app.common.pageSuffix');

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        inputRef.current?.focus();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);

  useEffect(() => setMobileOpen(false), [location.pathname]);

  const searchOptions = useMemo(() => [
    ...ROUTES.map((route) => {
      const label = t(route.labelKey);
      return { value: label, label: `${label} · ${pageSuffix}` };
    }),
    ...brokers.map((broker) => ({ value: broker.memberId, label: `${broker.memberId} · ${broker.address}` })),
  ], [brokers, t, pageSuffix]);

  const navigateFromMenu: MenuProps['onClick'] = ({ key }) => navigate(key);
  const navigation = (
    <Menu
      mode="inline"
      items={menuItems}
      selectedKeys={[location.pathname]}
      onClick={navigateFromMenu}
      className="navigation-menu"
    />
  );

  const clusterCard = (
    <div className="sider-cluster-card">
      <div className="mini-topology" aria-hidden="true">
        <span className="mini-topology__line" />
        <i /><i /><i />
      </div>
      <strong>Kunpeng Cluster</strong>
      <span><i className="live-dot" /> {unhealthy.length ? t('app.sider.replicasAttention', { count: unhealthy.length }) : t('app.sider.allHealthy')}</span>
      <small>{t('app.sider.stats', { nodes: brokers.length, partitions: logicalPartitions })}</small>
    </div>
  );

  const doLogout = async () => {
    try {
      await logout();
    } finally {
      onLoggedOut();
      navigate('/login', { replace: true });
    }
  };

  return (
    <Layout className="app-shell">
      <Sider
        className="desktop-sider"
        theme="light"
        width={236}
        collapsedWidth={82}
        collapsed={collapsed}
        trigger={null}
      >
        <div className="sider-inner">
          <ClusterBrand compact={collapsed} />
          {!collapsed && <div className="sider-section-label">{t('app.menu.console')}</div>}
          {navigation}
          <div className="sider-bottom">
            {!collapsed && clusterCard}
            <button className="collapse-button" onClick={() => setCollapsed((value) => !value)} aria-label={collapsed ? t('app.menu.expandNav') : t('app.menu.collapseNav')}>
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              {!collapsed && <span>{t('app.menu.collapseNav')}</span>}
            </button>
          </div>
        </div>
      </Sider>

      <Drawer
        className="mobile-navigation"
        placement="left"
        width={278}
        open={mobileOpen}
        onClose={() => setMobileOpen(false)}
        closable={false}
        styles={{ body: { padding: 0 } }}
      >
        <div className="mobile-navigation__inner">
          <ClusterBrand />
          <div className="sider-section-label">{t('app.menu.console')}</div>
          {navigation}
          {clusterCard}
        </div>
      </Drawer>

      <Layout className="main-layout">
        <Header className="app-header">
          <div className="app-header__title">
            <Button className="mobile-menu-button" type="text" icon={<MenuOutlined />} onClick={() => setMobileOpen(true)} aria-label={t('app.menu.openNav')} />
            <div>
              <strong>{currentRoute.title}</strong>
              <span>{currentRoute.subtitle}</span>
            </div>
          </div>
          <div className="app-header__actions">
            <AutoComplete
              className="global-search"
              value={search}
              options={searchOptions}
              onChange={setSearch}
              filterOption={(input, option) => String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())}
              onSelect={(value) => {
                navigate(searchRoutes[value] ?? `/nodes?keyword=${encodeURIComponent(value)}`);
                setSearch('');
              }}
            >
              <Input ref={inputRef} prefix={<SearchOutlined />} suffix={<kbd>⌘ K</kbd>} placeholder={t('app.header.searchPlaceholder')} variant="filled" aria-label={t('app.header.searchAria')} />
            </AutoComplete>
            <div className="header-cluster-status"><i className="live-dot" /><span>{t('app.header.cluster')}</span><b>{unhealthy.length ? t('app.header.attention') : t('app.header.running')}</b></div>
            <Dropdown
              trigger={['click']}
              placement="bottomRight"
              dropdownRender={() => (
                <div className="notification-popover">
                  <div className="notification-popover__header"><strong>{t('app.header.notificationCenter')}</strong><span>{unhealthy.length ? t('app.header.unreadCount', { count: unhealthy.length }) : t('app.header.noUnread')}</span></div>
                  {unhealthy.length === 0 && (
                    <div className="notification-item"><span className="notification-item__icon success"><CheckCircleFilled /></span><div><strong>{t('app.header.clusterHealthy')}</strong><p>{t('app.header.allPartitionsHealthy')}</p><small>{t('app.header.justNow')}</small></div></div>
                  )}
                  {unhealthy.slice(0, 4).map((item) => (
                    <div className="notification-item" key={`${item.group}-${item.id}-${item.broker.memberId}`}>
                      <span className="notification-item__icon warning"><WarningFilled /></span>
                      <div><strong>{t('app.header.partitionHealthTitle')}</strong><p>{t('app.header.partitionHealthBody', { member: item.broker.memberId, partition: `${item.group}-${item.id}` })}</p><small>{t('app.header.seeMatrix')}</small></div>
                    </div>
                  ))}
                  <button onClick={() => navigate('/partitions')}>{t('app.header.viewClusterStatus')} <span>→</span></button>
                </div>
              )}
            >
              <Badge count={unhealthy.length} size="small" offset={[-3, 4]}><Button className="header-icon-button" type="text" icon={<BellOutlined />} aria-label={t('app.header.notifications')} /></Badge>
            </Dropdown>
            <ThemeToggle />
            <LanguageToggle />
            <span className="header-divider" />
            <Dropdown menu={{ items: [
              { key: 'logout', icon: <LogoutOutlined />, label: t('app.header.logout') },
            ], onClick: ({ key }) => {
              if (key === 'logout') {
                void doLogout();
              }
            } }} placement="bottomRight">
              <button className="admin-profile"><Avatar size={36} className="admin-avatar">{user.username.charAt(0).toUpperCase()}</Avatar><span><strong>{user.username}</strong><small>{user.roles[0] ?? t('app.header.fallbackRole')}</small></span><DownOutlined /></button>
            </Dropdown>
          </div>
        </Header>
        <Content className="app-content">
          <div className="route-frame" key={location.pathname}><Outlet /></div>
          <footer className="app-footer">
            <span>KUNPENG CLUSTER CONSOLE</span>
            {user.filingInfo && <span>{user.filingInfo}</span>}
            <span>{t('app.footer.tagline')}</span>
          </footer>
        </Content>
      </Layout>
    </Layout>
  );
}
