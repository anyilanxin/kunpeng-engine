import { useEffect, useMemo, useRef, useState } from 'react';
import { AutoComplete, Avatar, Badge, Button, Drawer, Dropdown, Input, Layout, Menu } from 'antd';
import type { InputRef, MenuProps } from 'antd';
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
import { useBrokers } from '../data/ClusterDataContext';
import { logout } from '../api';
import type { MeInfo } from '../api';
import { getPartitionCells } from '../utils/cluster';

const { Header, Sider, Content } = Layout;

const menuItems: NonNullable<MenuProps['items']> = [
  { key: '/', icon: <DashboardOutlined />, label: '集群工作台' },
  { key: '/nodes', icon: <ClusterOutlined />, label: '节点管理' },
  { key: '/partitions', icon: <PartitionOutlined />, label: '业务分区管理' },
  { key: '/admin-partitions', icon: <ControlOutlined />, label: '管理分区管理' },
  { key: '/workers', icon: <ApiOutlined />, label: 'JobWorker 管理' },
  { key: '/dispatch', icon: <SwapOutlined />, label: '调度中心' },
  { key: '/gateways', icon: <GatewayOutlined />, label: 'Gateway 管理' },
];

const routeTitles: Record<string, { title: string; subtitle: string }> = {
  '/': { title: '集群工作台', subtitle: 'Cluster overview' },
  '/nodes': { title: '节点管理', subtitle: 'Broker nodes' },
  '/partitions': { title: '业务分区管理', subtitle: 'Business raft partitions' },
  '/admin-partitions': { title: '管理分区管理', subtitle: 'Admin partitions' },
  '/workers': { title: 'JobWorker 管理', subtitle: 'Job orchestration' },
  '/dispatch': { title: '调度中心', subtitle: 'Dispatch plans & cluster meta' },
  '/gateways': { title: 'Gateway 管理', subtitle: 'Gateway discovery & load' },
};

const searchRoutes: Record<string, string> = {
  '集群工作台': '/',
  '节点管理': '/nodes',
  '业务分区管理': '/partitions',
  '管理分区管理': '/admin-partitions',
  'JobWorker 管理': '/workers',
  '调度中心': '/dispatch',
  'Gateway 管理': '/gateways',
};

export default function AdminLayout({ user, onLoggedOut }: { user: MeInfo; onLoggedOut: () => void }) {
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
  const currentRoute = routeTitles[location.pathname] ?? routeTitles['/'];

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
    ...Object.keys(searchRoutes).map((label) => ({ value: label, label: `${label} · 页面` })),
    ...brokers.map((broker) => ({ value: broker.memberId, label: `${broker.memberId} · ${broker.address}` })),
  ], [brokers]);

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
      <span><i className="live-dot" /> {unhealthy.length ? `${unhealthy.length} 个副本需要关注` : '所有系统运行正常'}</span>
      <small>{brokers.length} 个节点 · {logicalPartitions} 个分区</small>
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
          {!collapsed && <div className="sider-section-label">集群控制台</div>}
          {navigation}
          <div className="sider-bottom">
            {!collapsed && clusterCard}
            <button className="collapse-button" onClick={() => setCollapsed((value) => !value)} aria-label={collapsed ? '展开侧边栏' : '收起侧边栏'}>
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              {!collapsed && <span>收起导航</span>}
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
          <div className="sider-section-label">集群控制台</div>
          {navigation}
          {clusterCard}
        </div>
      </Drawer>

      <Layout className="main-layout">
        <Header className="app-header">
          <div className="app-header__title">
            <Button className="mobile-menu-button" type="text" icon={<MenuOutlined />} onClick={() => setMobileOpen(true)} aria-label="打开导航" />
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
              <Input ref={inputRef} prefix={<SearchOutlined />} suffix={<kbd>⌘ K</kbd>} placeholder="搜索节点、分区或 Worker" variant="filled" aria-label="全局搜索" />
            </AutoComplete>
            <div className="header-cluster-status"><i className="live-dot" /><span>Kunpeng 集群</span><b>{unhealthy.length ? '需关注' : '运行中'}</b></div>
            <Dropdown
              trigger={['click']}
              placement="bottomRight"
              dropdownRender={() => (
                <div className="notification-popover">
                  <div className="notification-popover__header"><strong>通知中心</strong><span>{unhealthy.length ? `${unhealthy.length} 条待关注` : '暂无未读'}</span></div>
                  {unhealthy.length === 0 && (
                    <div className="notification-item"><span className="notification-item__icon success"><CheckCircleFilled /></span><div><strong>集群状态正常</strong><p>所有分区副本健康</p><small>刚刚</small></div></div>
                  )}
                  {unhealthy.slice(0, 4).map((item) => (
                    <div className="notification-item" key={`${item.group}-${item.id}-${item.broker.memberId}`}>
                      <span className="notification-item__icon warning"><WarningFilled /></span>
                      <div><strong>分区健康度需要关注</strong><p>{item.broker.memberId} 上的 {item.group}-{item.id} 副本异常</p><small>见分区矩阵</small></div>
                    </div>
                  ))}
                  <button onClick={() => navigate('/partitions')}>查看集群状态 <span>→</span></button>
                </div>
              )}
            >
              <Badge count={unhealthy.length} size="small" offset={[-3, 4]}><Button className="header-icon-button" type="text" icon={<BellOutlined />} aria-label="通知" /></Badge>
            </Dropdown>
            <span className="header-divider" />
            <Dropdown menu={{ items: [
              { key: 'logout', icon: <LogoutOutlined />, label: '退出登录' },
            ], onClick: ({ key }) => {
              if (key === 'logout') {
                void doLogout();
              }
            } }} placement="bottomRight">
              <button className="admin-profile"><Avatar size={36} className="admin-avatar">{user.username.charAt(0).toUpperCase()}</Avatar><span><strong>{user.username}</strong><small>{user.roles[0] ?? '集群管理员'}</small></span><DownOutlined /></button>
            </Dropdown>
          </div>
        </Header>
        <Content className="app-content">
          <div className="route-frame" key={location.pathname}><Outlet /></div>
          <footer className="app-footer">
            <span>KUNPENG CLUSTER CONSOLE</span>
            {user.filingInfo && <span>{user.filingInfo}</span>}
            <span>微服务编排引擎 · 集群管理</span>
          </footer>
        </Content>
      </Layout>
    </Layout>
  );
}
