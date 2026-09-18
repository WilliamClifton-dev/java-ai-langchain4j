import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, ArrowRight, CheckCircle2, RotateCcw } from 'lucide-react';
import { useRef, useState } from 'react';

import type { HbtiDefinition, HbtiResult } from '../api/domain';
import { api, isApiError } from '../api/http';

const VERSION = '1.0.0';
const SCALE = [1, 2, 3, 4, 5];

function idempotencyKey() {
  return globalThis.crypto?.randomUUID?.() ?? `assessment-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export function AssessmentPage() {
  const queryClient = useQueryClient();
  const definitionQuery = useQuery({ queryKey: ['hbti', 'definition', VERSION], queryFn: () => api.getHbtiDefinition(VERSION) });
  const resultQuery = useQuery({ queryKey: ['hbti', 'result'], queryFn: api.getCurrentHbtiResult });
  const [answers, setAnswers] = useState<Record<string, number>>({});
  const [questionIndex, setQuestionIndex] = useState(0);
  const [started, setStarted] = useState(false);
  const [localResult, setLocalResult] = useState<HbtiResult | null>(null);
  const [validationMessage, setValidationMessage] = useState<string | null>(null);
  const submissionKey = useRef<string | null>(null);

  const submitMutation = useMutation({
    mutationFn: () => api.submitHbti(VERSION, Object.entries(answers).map(([itemKey, value]) => ({ itemKey, value })), submissionKey.current ?? (submissionKey.current = idempotencyKey())),
    onSuccess: (response) => {
      setLocalResult(response.result);
      queryClient.setQueryData(['hbti', 'result'], response.result);
      setStarted(false);
      submissionKey.current = null;
    },
  });

  const definition = definitionQuery.data;
  const result = localResult ?? resultQuery.data;
  const item = definition?.items[questionIndex];

  if (definitionQuery.isLoading) return <main className="centered-state"><span className="spinner" /><p>正在准备测评</p></main>;
  if (definitionQuery.isError) return <main className="centered-state" role="alert"><h1>测评暂时不可用</h1><p>题目版本读取失败，请稍后重试。</p></main>;

  function choose(value: number) {
    if (!item) return;
    setAnswers((current) => ({ ...current, [item.itemKey]: value }));
    setValidationMessage(null);
    if (questionIndex < definition!.items.length - 1) {
      setQuestionIndex((current) => current + 1);
    }
  }

  function submit() {
    if (!item || !answers[item.itemKey]) {
      setValidationMessage('请选择一个最符合当前情况的选项');
      return;
    }
    submitMutation.mutate();
  }

  function back() {
    setQuestionIndex((current) => Math.max(0, current - 1));
    setValidationMessage(null);
  }

  if (result && !started) {
    return <ResultView result={result} definition={definition!} onRetake={() => { setAnswers({}); setQuestionIndex(0); setLocalResult(null); setStarted(true); }} />;
  }

  return (
    <main className="workspace workspace-wide assessment-page">
      <header className="page-heading">
        <div><p className="eyebrow">HBTI · {VERSION}</p><h1>HBTI 行为倾向测评</h1></div>
        <p>{definition!.limitation}</p>
      </header>
      {!started ? (
        <section className="assessment-start data-section">
          <div className="assessment-start-copy"><CheckCircle2 size={24} /><div><h2>{resultQuery.isError && isApiError(resultQuery.error, 'ASSESSMENT_RESULT_NOT_FOUND') ? '完成一次新的自我观察' : '继续你的测评记录'}</h2><p>共 {definition!.items.length} 题，按近一段时间的真实体验回答。结果用于探索行为倾向，不用于诊断或处方。</p></div></div>
          <button className="button button-primary" type="button" onClick={() => setStarted(true)}>开始测评 <ArrowRight size={17} /></button>
        </section>
      ) : (
        <section className="assessment-run" aria-labelledby="question-title">
          <div className="progress-meta"><span>问题 {questionIndex + 1} / {definition!.items.length}</span><span>{Math.round(((questionIndex + 1) / definition!.items.length) * 100)}%</span></div>
          <div className="progress-track"><span style={{ width: `${((questionIndex + 1) / definition!.items.length) * 100}%` }} /></div>
          <div className="question-block">
            <p className="eyebrow">{item?.hintZh}</p><h2 id="question-title">{item?.titleZh}</h2>
            {item?.itemKey === 'q4' && <p className="question-explanation">这里的“反应特别强”是指：看到或吃到这类食物后，容易产生明显的想吃、继续吃或优先选择它们的冲动。</p>}
            <fieldset className="scale-fieldset"><legend>符合程度</legend><div className="scale-options">{SCALE.map((value) => <label className={answers[item!.itemKey] === value ? 'scale-option selected' : 'scale-option'} key={value}><input type="radio" name={item!.itemKey} value={value} checked={answers[item!.itemKey] === value} onChange={() => choose(value)} /><span>{value}</span></label>)}</div><div className="scale-legend"><span>完全不符合</span><span>非常符合</span></div></fieldset>
            <p className="question-flow-hint">选择后自动进入下一题；可以用“上一题”返回修改。</p>
            {validationMessage && <p className="form-error" role="alert">{validationMessage}</p>}
            {submitMutation.isError && <p className="form-error" role="alert">{isApiError(submitMutation.error) ? submitMutation.error.message : '提交失败，请保留当前答案后重试'}</p>}
          </div>
          <div className="assessment-actions"><button className="button button-secondary" type="button" onClick={back} disabled={questionIndex === 0}><ArrowLeft size={17} />上一题</button>{questionIndex === definition!.items.length - 1 && <button className="button button-primary" type="button" onClick={submit} disabled={submitMutation.isPending}>{submitMutation.isPending ? '正在计算' : '提交测评'}<ArrowRight size={17} /></button>}</div>
        </section>
      )}
    </main>
  );
}

function ResultView({ result, definition, onRetake }: { result: HbtiResult; definition: HbtiDefinition; onRetake: () => void }) {
  return <main className="workspace workspace-wide result-page"><header className="page-heading"><div><p className="eyebrow">测评结果</p><h1>HBTI 维度画像</h1></div><div className="result-heading-meta"><p>{result.limitation}</p><div className="type-summary"><span>辅助类型代码</span><strong>{result.typeCode}</strong></div></div></header><section className="result-list" aria-label="HBTI 维度结果">{result.dimensions.map((dimension) => { const definitionDimension = definition.dimensions.find((entry) => entry.code === dimension.dimensionCode); const total = dimension.leftScore + dimension.rightScore || 1; return <article className="result-row" key={dimension.dimensionCode}><div className="result-row-header"><strong>{dimension.dimensionCode}</strong><span>{dimension.chosenPole}</span></div><div className="dimension-labels"><span>{definitionDimension?.leftLabel ?? '左侧倾向'}</span><span>{definitionDimension?.rightLabel ?? '右侧倾向'}</span></div><div className="dimension-bar"><span style={{ width: `${(dimension.leftScore / total) * 100}%` }} /></div><div className="dimension-scores"><span>{dimension.leftScore}</span><span>{dimension.rightScore}</span></div></article>; })}</section><div className="result-note"><CheckCircle2 size={19} /><span>这是一份可复盘的行为观察结果，不是固定标签。你的实际体验可能随环境、睡眠和压力变化。</span></div><button className="button button-secondary" type="button" onClick={onRetake}><RotateCcw size={17} />重新测评</button></main>;
}
