import { ArrowRight, Eye, EyeOff, LockKeyhole } from 'lucide-react';
import { useId, useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';

import { ApiError } from '../api/http';
import { useAuth } from '../auth/AuthContext';

interface AuthPageProps {
  mode: 'login' | 'register';
}

const ERROR_MESSAGES: Record<string, string> = {
  INVALID_CREDENTIALS: '邮箱或密码不正确',
  INVALID_CREDENTIAL_INPUT: '请检查邮箱格式，密码需为 12 至 128 个字符',
  EMAIL_ALREADY_REGISTERED: '该邮箱已经注册，请直接登录',
  LOGIN_RATE_LIMITED: '尝试次数过多，请稍后再试',
  FORBIDDEN: '安全校验已失效，请刷新页面后重试',
};

export function AuthPage({ mode }: AuthPageProps) {
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const emailId = useId();
  const passwordId = useId();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const requestedLocation = (location.state as {
    from?: { pathname?: string; search?: string; hash?: string };
  } | null)?.from;
  const requestedPath = requestedLocation
    ? `${requestedLocation.pathname ?? '/'}${requestedLocation.search ?? ''}${requestedLocation.hash ?? ''}`
    : '/';

  if (auth.isInitializing) {
    return (
      <main className="centered-state" aria-live="polite">
        <span className="spinner" aria-hidden="true" />
        <p>正在恢复会话</p>
      </main>
    );
  }

  if (auth.session) {
    return <Navigate to={requestedPath} replace />;
  }

  const isLogin = mode === 'login';

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      const credentials = { email: email.trim(), password };
      if (isLogin) {
        await auth.login(credentials);
      } else {
        await auth.register(credentials);
      }
      navigate(requestedPath, { replace: true });
    } catch (cause) {
      if (cause instanceof ApiError) {
        setError(ERROR_MESSAGES[cause.code] ?? cause.message);
      } else {
        setError('暂时无法连接服务，请稍后重试');
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="auth-layout">
      <section className="auth-intro" aria-labelledby="auth-product-name">
        <div className="brand-lockup brand-lockup-large">
          <span className="brand-mark">H</span>
          <span id="auth-product-name">HBTI Coach</span>
        </div>
        <div className="auth-statement">
          <p className="eyebrow">个性化体重管理</p>
          <h1>理解自己的节奏，建立可持续的行动。</h1>
          <p>从行为倾向出发，把饮食、训练和日常记录整理成清晰的下一步。</p>
        </div>
        <p className="scope-note">探索性工具，不提供医疗诊断或治疗建议。</p>
      </section>

      <section className="auth-panel" aria-labelledby="auth-heading">
        <div className="auth-form-wrap">
          <LockKeyhole className="auth-icon" size={22} aria-hidden="true" />
          <p className="eyebrow">{isLogin ? '欢迎回来' : '创建账户'}</p>
          <h2 id="auth-heading">{isLogin ? '登录你的工作区' : '开始建立个人档案'}</h2>
          <p className="form-lead">{isLogin ? '继续查看你的测评、计划与记录。' : '你的数据只用于提供个人化体验。'}</p>

          <form onSubmit={handleSubmit} className="auth-form">
            <div className="field">
              <label htmlFor={emailId}>邮箱</label>
              <input
                id={emailId}
                name="email"
                type="email"
                autoComplete="email"
                maxLength={254}
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor={passwordId}>密码</label>
              <div className="password-field">
                <input
                  id={passwordId}
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete={isLogin ? 'current-password' : 'new-password'}
                  minLength={12}
                  maxLength={128}
                  required
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  aria-describedby={isLogin ? undefined : `${passwordId}-hint`}
                />
                <button
                  className="password-toggle"
                  type="button"
                  onClick={() => setShowPassword((visible) => !visible)}
                  aria-label={showPassword ? '隐藏密码' : '显示密码'}
                  title={showPassword ? '隐藏密码' : '显示密码'}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {!isLogin && <span className="field-hint" id={`${passwordId}-hint`}>至少 12 个字符</span>}
            </div>

            {error && <div className="form-error" role="alert">{error}</div>}

            <button className="button button-primary button-submit" type="submit" disabled={isSubmitting}>
              <span>{isSubmitting ? '正在处理' : isLogin ? '登录' : '创建账户'}</span>
              {!isSubmitting && <ArrowRight size={18} aria-hidden="true" />}
            </button>
          </form>

          <p className="auth-switch">
            {isLogin ? '还没有账户？' : '已经有账户？'}{' '}
            <Link to={isLogin ? '/register' : '/login'}>{isLogin ? '立即注册' : '返回登录'}</Link>
          </p>
        </div>
      </section>
    </main>
  );
}
