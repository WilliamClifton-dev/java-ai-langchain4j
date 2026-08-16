import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react';

import { ApiError, api, type AuthCredentials, type AuthSession } from '../api/http';

interface AuthContextValue {
  session: AuthSession | null;
  isInitializing: boolean;
  initializationError: string | null;
  login(credentials: AuthCredentials): Promise<void>;
  register(credentials: AuthCredentials): Promise<void>;
  logout(): Promise<void>;
  retryInitialization(): Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function shouldTryRefresh(error: unknown) {
  return error instanceof ApiError && error.status === 401;
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);
  const [initializationError, setInitializationError] = useState<string | null>(null);

  const initialize = useCallback(async () => {
    setIsInitializing(true);
    setInitializationError(null);
    try {
      setSession(await api.getSession());
    } catch (error) {
      if (!shouldTryRefresh(error)) {
        setSession(null);
        setInitializationError('暂时无法连接服务，请检查网络后重试');
        return;
      }

      try {
        await api.refresh();
        setSession(await api.getSession());
      } catch (refreshError) {
        if (shouldTryRefresh(refreshError)) {
          setSession(null);
        } else {
          setSession(null);
          setInitializationError('暂时无法恢复会话，请稍后重试');
        }
      }
    } finally {
      setIsInitializing(false);
    }
  }, []);

  useEffect(() => {
    void initialize();
  }, [initialize]);

  const value = useMemo<AuthContextValue>(() => ({
    session,
    isInitializing,
    initializationError,
    async login(credentials) {
      setSession(await api.login(credentials));
    },
    async register(credentials) {
      setSession(await api.register(credentials));
    },
    async logout() {
      await api.logout();
      setSession(null);
    },
    retryInitialization: initialize,
  }), [initialize, initializationError, isInitializing, session]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}
