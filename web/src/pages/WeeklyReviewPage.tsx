import { useMutation } from '@tanstack/react-query';
import { AlertTriangle, BarChart3, CalendarDays, CheckCircle2 } from 'lucide-react';
import { useState } from 'react';

import type { WeeklyReview } from '../api/domain';
import { api, isApiError } from '../api/http';

function today() {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
}

const recommendationLabels: Record<WeeklyReview['recommendation'], string> = {
  INSUFFICIENT_DATA: '数据不足', HOLD: '保持当前方案', INCREASE_ENERGY: '建议评估增加能量', DECREASE_ENERGY: '建议评估减少能量',
};

export function WeeklyReviewPage() {
  const [windowEnd, setWindowEnd] = useState(today);
  const reviewMutation = useMutation({ mutationFn: () => api.generateWeeklyReview(windowEnd) });
  const result = reviewMutation.data;
  const review = result?.review;

  return <main className="workspace workspace-wide review-page">
    <header className="page-heading">
      <div><p className="eyebrow">确定性趋势 · 七日窗口</p><h1>七日回顾</h1></div>
      <p>服务端根据已记录事实计算趋势、覆盖率和依从性。单日波动不会直接触发计划调整。</p>
    </header>
    <section className="review-generator data-section">
      <div className="section-heading"><span className="step-index"><CalendarDays size={21} /></span><div><h2>选择窗口结束日</h2><p>系统将读取该日期及之前六天。</p></div></div>
      <div className="review-controls"><label className="field" htmlFor="review-end"><span>窗口结束日</span><input id="review-end" type="date" value={windowEnd} max={today()} onChange={(event) => setWindowEnd(event.target.value)} /></label><button className="button button-primary" type="button" onClick={() => reviewMutation.mutate()} disabled={reviewMutation.isPending}><BarChart3 size={17} />{reviewMutation.isPending ? '正在生成' : '生成七日回顾'}</button></div>
      {reviewMutation.isError && <div className="notice notice-error" role="alert"><AlertTriangle size={17} />{isApiError(reviewMutation.error) ? reviewMutation.error.message : '暂时无法生成回顾，请稍后重试'}</div>}
    </section>
    {review && <ReviewResult review={review} replayed={result.replayed} />}
  </main>;
}

function ReviewResult({ review, replayed }: { review: WeeklyReview; replayed: boolean }) {
  return <section className="data-section review-result" aria-live="polite">
    <header className="review-result-heading"><div><p className="eyebrow">{review.windowStart} 至 {review.windowEnd} · v{review.versionNo}</p><h2>{recommendationLabels[review.recommendation]}</h2></div><span className="review-replay">{replayed ? '相同数据 · 已复用' : '新生成'}</span></header>
    <div className="review-metrics">
      <ReviewMetric label="体重记录" value={`${review.weightObservationDays}/7 天`} />
      <ReviewMetric label="体重趋势" value={review.weightTrendPercent == null ? '数据不足' : `${review.weightTrendPercent > 0 ? '+' : ''}${review.weightTrendPercent}%`} />
      <ReviewMetric label="营养记录" value={`${review.nutritionLoggedDays}/7 天`} />
      <ReviewMetric label="范围依从性" value={review.nutritionAdherencePercent == null ? '未计算' : `${review.nutritionAdherencePercent}%`} />
      <ReviewMetric label="平均步数" value={review.averageSteps == null ? '数据不足' : String(review.averageSteps)} />
      <ReviewMetric label="训练总时长" value={`${review.totalTrainingMinutes} 分钟`} />
    </div>
    <div className="review-proposal"><CheckCircle2 size={20} /><div><strong>{review.proposedEnergyDeltaKcalPerDay === 0 ? '本次无能量调整提案' : `能量调整提案 ${review.proposedEnergyDeltaKcalPerDay > 0 ? '+' : ''}${review.proposedEnergyDeltaKcalPerDay} kcal/日`}</strong><p>这是待评估提案，服务端不会自动修改当前计划。</p></div></div>
  </section>;
}

function ReviewMetric({ label, value }: { label: string; value: string }) {
  return <div><span>{label}</span><strong>{value}</strong></div>;
}
