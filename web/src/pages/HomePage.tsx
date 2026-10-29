import { Activity, CheckCircle2, ClipboardList, Clock3, LockKeyhole, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';

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

      <section className="next-step-section" aria-labelledby="next-step-heading">
        <div>
          <p className="eyebrow">继续你的工作区</p>
          <h2 id="next-step-heading">从一个小步骤开始</h2>
          <p className="next-step-lead">完善基础信息后，系统才能计算计划并生成有上下文的回顾。</p>
        </div>
        <div className="next-step-list">
          <Link className="next-step-item" to="/profile">
            <ClipboardList size={20} aria-hidden="true" />
            <span><strong>完善个人档案</strong><small>保存计算所需的基本信息与安全筛查</small></span>
          </Link>
          <Link className="next-step-item" to="/assessment">
            <Sparkles size={20} aria-hidden="true" />
            <span><strong>完成 HBTI 测评</strong><small>用 16 个问题开始一次行为倾向自我观察</small></span>
          </Link>
          <Link className="next-step-item" to="/tracking">
            <Activity size={20} aria-hidden="true" />
            <span><strong>记录今天</strong><small>留下体重、饮食或训练事实，供后续回顾使用</small></span>
          </Link>
        </div>
      </section>
    </main>
  );
}
