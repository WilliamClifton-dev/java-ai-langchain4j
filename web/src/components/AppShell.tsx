import { ClipboardList, Home, LogOut, ShieldCheck, Sparkles, Target } from 'lucide-react';
import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';

import { useAuth } from '../auth/AuthContext';

export function AppShell() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState<string | null>(null);

  async function handleLogout() {
    setLogoutError(null);
    setIsLoggingOut(true);
    try {
      await auth.logout();
      navigate('/login', { replace: true });
    } catch {
      setLogoutError('未能安全退出，当前会话仍然有效，请重试');
    } finally {
      setIsLoggingOut(false);
    }
  }

  return (
    <div className="app-frame">
      <header className="topbar">
        <div className="brand-lockup" aria-label="HBTI Coach">
          <span className="brand-mark">H</span>
          <span>HBTI Coach</span>
        </div>
        <div className="account-actions">
          <span className="account-email">{auth.session?.user.email}</span>
          <button
            className="icon-button"
            type="button"
            onClick={() => void handleLogout()}
            disabled={isLoggingOut}
            aria-label={isLoggingOut ? '正在退出' : '退出登录'}
            title="退出登录"
          >
            <LogOut size={18} aria-hidden="true" />
          </button>
        </div>
      </header>
      <nav className="primary-nav" aria-label="主要导航">
        <NavLink to="/" end><Home size={17} />概览</NavLink>
        <NavLink to="/profile"><ClipboardList size={17} />档案</NavLink>
        <NavLink to="/assessment"><Sparkles size={17} />测评</NavLink>
        <NavLink to="/plan"><Target size={17} />计划</NavLink>
      </nav>
      <div className="context-bar">
        <ShieldCheck size={16} aria-hidden="true" />
        <span>探索性行为倾向工具，不替代医疗诊断</span>
      </div>
      {logoutError && <div className="shell-error" role="alert">{logoutError}</div>}
      <Outlet />
    </div>
  );
}
