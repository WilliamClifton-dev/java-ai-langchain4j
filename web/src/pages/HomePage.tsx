import { CheckCircle2, Clock3, LockKeyhole } from 'lucide-react';

import { useAuth } from '../auth/AuthContext';

export function HomePage() {
  const { session } = useAuth();
  const expiresAt = session?.accessExpiresAt
    ? new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(new Date(session.accessExpiresAt))
    : '--:--';

  return (
    <main className="workspace">
      <header className="workspace-heading">
        <p className="eyebrow">账户工作区</p>
        <h1>晚上好</h1>
        <p>你的安全会话已恢复，可以继续上次的进度。</p>
      </header>

      <section className="status-grid" aria-label="账户状态">
        <article className="status-item">
          <CheckCircle2 size={20} aria-hidden="true" />
          <div>
            <span className="status-label">连接状态</span>
            <strong>已连接</strong>
          </div>
        </article>
        <article className="status-item">
          <Clock3 size={20} aria-hidden="true" />
          <div>
            <span className="status-label">本次会话</span>
            <strong>{expiresAt} 前有效</strong>
          </div>
        </article>
        <article className="status-item">
          <LockKeyhole size={20} aria-hidden="true" />
          <div>
            <span className="status-label">登录方式</span>
            <strong>安全 Cookie</strong>
          </div>
        </article>
      </section>

      <section className="account-section" aria-labelledby="account-heading">
        <div>
          <p className="eyebrow">当前账户</p>
          <h2 id="account-heading">身份信息</h2>
        </div>
        <dl className="account-details">
          <div>
            <dt>邮箱</dt>
            <dd>{session?.user.email}</dd>
          </div>
          <div>
            <dt>账户 ID</dt>
            <dd className="technical-value">{session?.user.id}</dd>
          </div>
        </dl>
      </section>
    </main>
  );
}
