import { Navigate, Outlet, useLocation } from 'react-router-dom';

import { useAuth } from './AuthContext';

export function ProtectedRoute() {
  const auth = useAuth();
  const location = useLocation();

  if (auth.isInitializing) {
    return (
      <main className="centered-state" aria-live="polite">
        <span className="spinner" aria-hidden="true" />
        <p>正在恢复会话</p>
      </main>
    );
  }

  if (auth.initializationError) {
    return (
      <main className="centered-state" role="alert">
        <h1>连接遇到问题</h1>
        <p>{auth.initializationError}</p>
        <button className="button button-primary" type="button" onClick={() => void auth.retryInitialization()}>
          重新连接
        </button>
      </main>
    );
  }

  if (!auth.session) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
