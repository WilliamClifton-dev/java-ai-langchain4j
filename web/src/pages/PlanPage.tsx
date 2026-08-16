import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, CheckCircle2, ClipboardCheck, Play, ShieldAlert } from 'lucide-react';
import { useRef, useState } from 'react';

import type { WeightPlan } from '../api/domain';
import { api, isApiError } from '../api/http';

const GOALS: Array<{ value: WeightPlan['goal']; label: string; detail: string }> = [
  { value: 'LOSS', label: '温和减重', detail: '以保守的周变化范围为参考' },
  { value: 'MAINTENANCE', label: '维持体重', detail: '围绕当前估算能量范围保持稳定' },
  { value: 'GAIN', label: '温和增重', detail: '以渐进方式支持体重增加' },
];

function keyFor(prefix: string) {
  return globalThis.crypto?.randomUUID?.() ?? `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function errorText(error: unknown) {
  if (!isApiError(error)) return '暂时无法完成操作，请稍后重试';
  if (error.code === 'PLANNING_PREREQUISITE_NOT_MET') {
    return '计划前置条件已变化，请重新检查档案、安全筛查和测评结果';
  }
  return error.message;
}

function statusLabel(status: WeightPlan['status']) {
  return ({ DRAFT: '草稿', VALIDATED: '已校验', CONFIRMED: '已确认', ACTIVE: '已启用', REPLACED: '已替换' })[status];
}

export function PlanPage() {
  const queryClient = useQueryClient();
  const profileQuery = useQuery({ queryKey: ['profile'], queryFn: api.getProfile });
  const screeningQuery = useQuery({ queryKey: ['screening', 'current'], queryFn: api.getCurrentScreening, enabled: Boolean(profileQuery.data) });
  const resultQuery = useQuery({ queryKey: ['hbti', 'result'], queryFn: api.getCurrentHbtiResult, enabled: Boolean(profileQuery.data) });
  const activeQuery = useQuery({ queryKey: ['plan', 'active'], queryFn: api.getActivePlan });
  const [goal, setGoal] = useState<WeightPlan['goal']>('LOSS');
  const [draft, setDraft] = useState<WeightPlan | null>(null);
  const [isCreatingReplacement, setIsCreatingReplacement] = useState(false);
  const draftKey = useRef<string | undefined>(undefined);
  const activationKey = useRef<string | undefined>(undefined);

  const createMutation = useMutation({
    mutationFn: () => api.createPlanDraft(goal, draftKey.current ?? (draftKey.current = keyFor('plan-draft'))),
    onSuccess: (created) => setDraft(created),
  });
  const transitionMutation = useMutation({
    mutationFn: (action: 'validation' | 'confirmation') => api.transitionPlan(draft!, action),
    onSuccess: (updated) => setDraft(updated),
  });
  const activateMutation = useMutation({
    mutationFn: () => api.activatePlan(draft!, activationKey.current ?? (activationKey.current = keyFor('plan-activation'))),
    onSuccess: async (active) => {
      setDraft(active);
      queryClient.setQueryData(['plan', 'active'], active);
      await queryClient.invalidateQueries({ queryKey: ['plan', 'active'] });
    },
  });

  const activePlan = activeQuery.data;
  const plan = draft ?? (isCreatingReplacement ? null : activePlan);
  const screening = screeningQuery.data;
  const prerequisitesReady = Boolean(profileQuery.data && resultQuery.data && screening?.automaticPlanningAllowed);
  const prerequisiteLoading = profileQuery.isLoading || screeningQuery.isLoading || resultQuery.isLoading || activeQuery.isLoading;
  const prerequisiteError = profileQuery.error && !isApiError(profileQuery.error, 'PROFILE_NOT_FOUND')
    ? profileQuery.error
    : screeningQuery.error && !isApiError(screeningQuery.error, 'PROFILE_NOT_FOUND')
      ? screeningQuery.error
      : resultQuery.error && !isApiError(resultQuery.error, 'ASSESSMENT_RESULT_NOT_FOUND')
        ? resultQuery.error
        : activeQuery.error && !isApiError(activeQuery.error, 'PLAN_VERSION_NOT_FOUND')
          ? activeQuery.error
          : null;

  if (prerequisiteLoading) return <main className="centered-state"><span className="spinner" /><p>正在读取计划前置条件</p></main>;
  if (prerequisiteError) return <main className="centered-state" role="alert"><h1>计划暂时不可用</h1><p>{errorText(prerequisiteError)}</p></main>;

  function transition(action: 'validation' | 'confirmation') {
    if (draft) transitionMutation.mutate(action);
  }

  const actionPending = createMutation.isPending || transitionMutation.isPending || activateMutation.isPending;

  return (
    <main className="workspace workspace-wide plan-page">
      <header className="page-heading">
        <div><p className="eyebrow">服务器规则 · 计划版本</p><h1>体重管理计划</h1></div>
        <p>计划目标和边界由服务端规则计算。这里展示的是规划估算，不是医疗处方或结果保证。</p>
      </header>

      <section className="plan-prerequisites" aria-label="计划前置条件">
        <Prerequisite label="个人档案" ready={Boolean(profileQuery.data)} detail={profileQuery.data ? '已保存' : '请先完善档案'} />
        <Prerequisite label="安全筛查" ready={Boolean(screening?.automaticPlanningAllowed)} detail={screening?.automaticPlanningAllowed ? '允许自动计划' : screening?.guidance ?? '尚未完成'} />
        <Prerequisite label="HBTI 测评" ready={Boolean(resultQuery.data)} detail={resultQuery.data ? `最近结果 ${resultQuery.data.typeCode}` : '请先完成测评'} />
      </section>

      {plan ? (
        <PlanSummary plan={plan} actionPending={actionPending} canReplace={plan.status === 'ACTIVE' && prerequisitesReady} onValidate={() => transition('validation')} onConfirm={() => transition('confirmation')} onActivate={() => activateMutation.mutate()} onReplace={() => setIsCreatingReplacement(true)} />
      ) : !prerequisitesReady ? (
        <section className="notice notice-warning plan-blocked" role="status">
          <ShieldAlert size={19} />
          <span><strong>自动计划已暂停</strong>请先完成安全筛查并确认可以进入自动计划。</span>
        </section>
      ) : (
        <section className="data-section plan-builder">
          <div className="section-heading"><span className="step-index">01</span><div><h2>选择计划方向</h2><p>只选择目标，数值和边界由服务端生成。</p></div></div>
          <fieldset className="goal-options">
            <legend className="sr-only">计划方向</legend>
            {GOALS.map((option) => <label className={goal === option.value ? 'goal-option selected' : 'goal-option'} key={option.value}><input aria-label={option.label} type="radio" name="goal" value={option.value} checked={goal === option.value} onChange={() => { setGoal(option.value); draftKey.current = undefined; }} disabled={createMutation.isPending} /><span><strong>{option.label}</strong><small>{option.detail}</small></span></label>)}
          </fieldset>
          {createMutation.isError && <div className="notice notice-error" role="alert"><AlertTriangle size={17} />{errorText(createMutation.error)}</div>}
          <div className="section-actions plan-builder-actions">{activePlan && <button className="button button-secondary" type="button" onClick={() => { setIsCreatingReplacement(false); draftKey.current = undefined; }} disabled={actionPending}>返回当前计划</button>}<button className="button button-primary" type="button" onClick={() => createMutation.mutate()} disabled={actionPending}><ClipboardCheck size={17} />{createMutation.isPending ? '正在生成' : '生成计划草稿'}</button></div>
        </section>
      )}
      {(transitionMutation.isError || activateMutation.isError) && <div className="notice notice-error" role="alert"><AlertTriangle size={17} />{errorText(transitionMutation.error ?? activateMutation.error)}</div>}
    </main>
  );
}

function Prerequisite({ label, ready, detail }: { label: string; ready: boolean; detail: string }) {
  return <article className={ready ? 'prerequisite ready' : 'prerequisite'}><span>{ready ? <CheckCircle2 size={18} /> : <AlertTriangle size={18} />}</span><div><strong>{label}</strong><small>{detail}</small></div></article>;
}

function PlanSummary({ plan, actionPending, canReplace, onValidate, onConfirm, onActivate, onReplace }: { plan: WeightPlan; actionPending: boolean; canReplace: boolean; onValidate: () => void; onConfirm: () => void; onActivate: () => void; onReplace: () => void }) {
  return <section className="data-section plan-summary">
    <div className="plan-summary-heading"><div><p className="eyebrow">{plan.goal === 'LOSS' ? '温和减重' : plan.goal === 'GAIN' ? '温和增重' : '维持体重'} · v{plan.versionNo}</p><h2>{plan.status === 'ACTIVE' ? '当前计划已启用' : '计划正在准备'}</h2></div><span className={`plan-status plan-status-${plan.status.toLowerCase()}`}>{statusLabel(plan.status)}</span></div>
    <div className="plan-metrics"><Metric label="BMI 估算" value={plan.bmi.toFixed(1)} suffix="" /><Metric label="基础代谢" value={String(plan.bmrKcalPerDay)} suffix="kcal/日" /><Metric label="总消耗估算" value={String(plan.tdeeKcalPerDay)} suffix="kcal/日" /><Metric label="目标能量范围" value={`${plan.energyMinKcalPerDay}–${plan.energyMaxKcalPerDay}`} suffix="kcal/日" /></div>
    <p className="plan-guidance">{plan.guidance}</p>
    {(plan.status !== 'ACTIVE' || canReplace) && <div className="plan-actions">{plan.status === 'DRAFT' && <button className="button button-primary" type="button" onClick={onValidate} disabled={actionPending}>校验计划</button>}{plan.status === 'VALIDATED' && <button className="button button-primary" type="button" onClick={onConfirm} disabled={actionPending}>确认计划</button>}{plan.status === 'CONFIRMED' && <button className="button button-primary" type="button" onClick={onActivate} disabled={actionPending}><Play size={17} />{actionPending ? '正在启用' : '启用计划'}</button>}{plan.status === 'ACTIVE' && canReplace && <button className="button button-secondary" type="button" onClick={onReplace}>制定新计划</button>}</div>}
  </section>;
}

function Metric({ label, value, suffix }: { label: string; value: string; suffix: string }) {
  return <div className="plan-metric"><span>{label}</span><strong>{value}</strong><small>{suffix}</small></div>;
}
