import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Activity, AlertTriangle, CheckCircle2, Dumbbell, Utensils } from 'lucide-react';
import { useRef, useState, type FormEvent } from 'react';

import type { DailyMetricInput, NutritionInput, TrainingInput } from '../api/domain';
import { api, isApiError } from '../api/http';

function today() {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
}

function keyFor(prefix: string) {
  return globalThis.crypto?.randomUUID?.() ?? `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function optionalNumber(value: string) {
  return value === '' ? undefined : Number(value);
}

function errorText(error: unknown) {
  if (!isApiError(error)) return '暂时无法保存记录，请稍后重试';
  if (error.code === 'TRACKING_DATE_CONFLICT') return '这一天已有同类汇总记录，请查看当天数据';
  if (error.code === 'TRACKING_IDEMPOTENCY_CONFLICT') return '本次提交内容已变化，请重新提交';
  return error.message;
}

export function TrackingPage() {
  const queryClient = useQueryClient();
  const [localDate, setLocalDate] = useState(today);
  const [metric, setMetric] = useState({ weightKg: '', steps: '', activityMinutes: '', sleepMinutes: '', sleepQuality: '' });
  const [nutrition, setNutrition] = useState({ energyKcal: '', proteinG: '', carbohydrateG: '', fatG: '' });
  const [training, setTraining] = useState<TrainingInput>({ localDate, trainingType: 'STRENGTH', durationMinutes: 45, intensity: 'MODERATE' });
  const metricKey = useRef<string | undefined>(undefined);
  const nutritionKey = useRef<string | undefined>(undefined);
  const trainingKey = useRef<string | undefined>(undefined);
  const summaryQuery = useQuery({ queryKey: ['tracking', 'day', localDate], queryFn: () => api.getDailySummary(localDate) });

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['tracking', 'day', localDate] });
  const metricMutation = useMutation({
    mutationFn: (input: DailyMetricInput) => api.recordDailyMetric(input, metricKey.current ?? (metricKey.current = keyFor('metric'))),
    onSuccess: async () => { metricKey.current = undefined; await refresh(); },
  });
  const nutritionMutation = useMutation({
    mutationFn: (input: NutritionInput) => api.recordNutrition(input, nutritionKey.current ?? (nutritionKey.current = keyFor('nutrition'))),
    onSuccess: async () => { nutritionKey.current = undefined; await refresh(); },
  });
  const trainingMutation = useMutation({
    mutationFn: (input: TrainingInput) => api.recordTraining(input, trainingKey.current ?? (trainingKey.current = keyFor('training'))),
    onSuccess: async () => { trainingKey.current = undefined; await refresh(); },
  });

  function changeDate(value: string) {
    setLocalDate(value);
    setTraining((current) => ({ ...current, localDate: value }));
    metricKey.current = undefined;
    nutritionKey.current = undefined;
    trainingKey.current = undefined;
  }

  function submitMetric(event: FormEvent) {
    event.preventDefault();
    const input = Object.fromEntries(Object.entries(metric).map(([name, value]) => [name, optionalNumber(value)])) as Omit<DailyMetricInput, 'localDate'>;
    metricMutation.mutate({ localDate, ...input });
  }

  function submitNutrition(event: FormEvent) {
    event.preventDefault();
    nutritionMutation.mutate({ localDate, energyKcal: Number(nutrition.energyKcal), proteinG: Number(nutrition.proteinG), carbohydrateG: Number(nutrition.carbohydrateG), fatG: Number(nutrition.fatG) });
  }

  return <main className="workspace workspace-wide tracking-page">
    <header className="page-heading">
      <div><p className="eyebrow">每日事实 · 明确单位</p><h1>每日执行记录</h1></div>
      <div className="date-control field"><label htmlFor="tracking-date">记录日期</label><input id="tracking-date" type="date" value={localDate} max={today()} onChange={(event) => changeDate(event.target.value)} /></div>
    </header>

    <DailySummaryPanel loading={summaryQuery.isLoading} error={summaryQuery.error} summary={summaryQuery.data} />

    <div className="tracking-form-grid">
      <form className="entry-form" onSubmit={submitMetric}>
        <FormHeading icon={<Activity size={19} />} title="身体与活动" detail="至少填写一项，时长使用分钟" />
        <div className="compact-form-grid">
          <NumberField label="体重（kg）" value={metric.weightKg} step="0.1" min="30" max="350" onChange={(value) => { setMetric((current) => ({ ...current, weightKg: value })); metricKey.current = undefined; }} />
          <NumberField label="步数（步）" value={metric.steps} min="0" max="100000" onChange={(value) => { setMetric((current) => ({ ...current, steps: value })); metricKey.current = undefined; }} />
          <NumberField label="活动（分钟）" value={metric.activityMinutes} min="0" max="1440" onChange={(value) => { setMetric((current) => ({ ...current, activityMinutes: value })); metricKey.current = undefined; }} />
          <NumberField label="睡眠（分钟）" value={metric.sleepMinutes} min="0" max="1440" onChange={(value) => { setMetric((current) => ({ ...current, sleepMinutes: value })); metricKey.current = undefined; }} />
          <NumberField label="睡眠质量（1–5）" value={metric.sleepQuality} min="1" max="5" onChange={(value) => { setMetric((current) => ({ ...current, sleepQuality: value })); metricKey.current = undefined; }} />
        </div>
        <MutationState mutation={metricMutation} />
        <button className="button button-primary" type="submit" disabled={metricMutation.isPending || Object.values(metric).every((value) => value === '')}>保存身体与活动记录</button>
      </form>

      <form className="entry-form" onSubmit={submitNutrition}>
        <FormHeading icon={<Utensils size={19} />} title="营养汇总" detail="每天一份汇总，能量为 kcal" />
        <div className="compact-form-grid">
          <NumberField required label="能量（kcal）" value={nutrition.energyKcal} min="0" max="10000" onChange={(value) => { setNutrition((current) => ({ ...current, energyKcal: value })); nutritionKey.current = undefined; }} />
          <NumberField required label="蛋白质（g）" value={nutrition.proteinG} step="0.1" min="0" max="1000" onChange={(value) => { setNutrition((current) => ({ ...current, proteinG: value })); nutritionKey.current = undefined; }} />
          <NumberField required label="碳水（g）" value={nutrition.carbohydrateG} step="0.1" min="0" max="1000" onChange={(value) => { setNutrition((current) => ({ ...current, carbohydrateG: value })); nutritionKey.current = undefined; }} />
          <NumberField required label="脂肪（g）" value={nutrition.fatG} step="0.1" min="0" max="1000" onChange={(value) => { setNutrition((current) => ({ ...current, fatG: value })); nutritionKey.current = undefined; }} />
        </div>
        <MutationState mutation={nutritionMutation} />
        <button className="button button-primary" type="submit" disabled={nutritionMutation.isPending}>保存营养汇总</button>
      </form>

      <form className="entry-form" onSubmit={(event) => { event.preventDefault(); trainingMutation.mutate(training); }}>
        <FormHeading icon={<Dumbbell size={19} />} title="训练记录" detail="同一天可记录多次训练" />
        <div className="compact-form-grid">
          <label className="field"><span>训练类型</span><select value={training.trainingType} onChange={(event) => { setTraining((current) => ({ ...current, trainingType: event.target.value as TrainingInput['trainingType'] })); trainingKey.current = undefined; }}><option value="STRENGTH">力量</option><option value="CARDIO">有氧</option><option value="MOBILITY">灵活性</option><option value="SPORT">运动</option><option value="OTHER">其他</option></select></label>
          <NumberField required label="时长（分钟）" value={String(training.durationMinutes)} min="1" max="600" onChange={(value) => { setTraining((current) => ({ ...current, durationMinutes: Number(value) })); trainingKey.current = undefined; }} />
          <label className="field"><span>强度</span><select value={training.intensity} onChange={(event) => { setTraining((current) => ({ ...current, intensity: event.target.value as TrainingInput['intensity'] })); trainingKey.current = undefined; }}><option value="LOW">低</option><option value="MODERATE">中等</option><option value="HIGH">高</option></select></label>
        </div>
        <MutationState mutation={trainingMutation} />
        <button className="button button-primary" type="submit" disabled={trainingMutation.isPending}>添加训练记录</button>
      </form>
    </div>
  </main>;
}

function FormHeading({ icon, title, detail }: { icon: React.ReactNode; title: string; detail: string }) {
  return <header className="entry-form-heading"><span>{icon}</span><div><h2>{title}</h2><p>{detail}</p></div></header>;
}

function NumberField({ label, value, onChange, required, ...input }: { label: string; value: string; onChange(value: string): void; required?: boolean; step?: string; min?: string; max?: string }) {
  const id = `field-${label}`;
  return <label className="field" htmlFor={id}><span>{label}</span><input id={id} type="number" value={value} required={required} onChange={(event) => onChange(event.target.value)} {...input} /></label>;
}

function MutationState({ mutation }: { mutation: { isError: boolean; isSuccess: boolean; error: unknown } }) {
  if (mutation.isError) return <div className="notice notice-error" role="alert"><AlertTriangle size={16} />{errorText(mutation.error)}</div>;
  if (mutation.isSuccess) return <div className="notice notice-success" role="status"><CheckCircle2 size={16} />记录已保存</div>;
  return null;
}

function DailySummaryPanel({ loading, error, summary }: { loading: boolean; error: unknown; summary?: Awaited<ReturnType<typeof api.getDailySummary>> }) {
  if (loading) return <section className="daily-summary inline-loading" aria-label="正在读取当日汇总"><span className="spinner" /><span>正在读取当日汇总</span></section>;
  if (error) return <section className="notice notice-error" role="alert"><AlertTriangle size={17} />{errorText(error)}</section>;
  if (!summary) return null;
  return <section className="daily-summary" aria-label="当日汇总">
    <div><span>体重</span><strong>{summary.metric?.weightKg != null ? `${summary.metric.weightKg} kg` : '未记录'}</strong></div>
    <div><span>步数</span><strong>{summary.metric?.steps != null ? `${summary.metric.steps} 步` : '未记录'}</strong></div>
    <div><span>能量</span><strong>{summary.nutrition ? `${summary.nutrition.energyKcal} kcal` : '未记录'}</strong></div>
    <div><span>训练</span><strong>{summary.trainingMinutes ? `${summary.trainingMinutes} 分钟` : '未记录'}</strong></div>
    <div><span>睡眠</span><strong>{summary.metric?.sleepMinutes != null ? `${summary.metric.sleepMinutes} 分钟` : '未记录'}</strong></div>
  </section>;
}
