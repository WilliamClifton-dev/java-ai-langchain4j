import { AlertTriangle, Bot, CircleStop, RotateCcw, Send } from 'lucide-react';
import { useEffect, useRef, useState, type FormEvent } from 'react';

import type { CoachScene, CoachStreamEvent, CoachStreamInput } from '../api/domain';
import { api, isApiError } from '../api/http';

const scenes: Array<{ value: CoachScene; label: string }> = [
  { value: 'GENERAL_CHAT', label: '一般讨论' }, { value: 'DAILY_CHECKIN', label: '每日复盘' },
  { value: 'WEEKLY_REVIEW', label: '周回顾解读' }, { value: 'PLAN_GENERATION', label: '计划讨论' },
  { value: 'HBTI_INTERPRETATION', label: 'HBTI 倾向解读' }, { value: 'SAFETY_SCREENING', label: '安全边界' },
];

function id(prefix: string) {
  return globalThis.crypto?.randomUUID?.() ?? `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export function CoachPage() {
  const conversationId = useRef(id('conversation'));
  const abort = useRef<AbortController | undefined>(undefined);
  const lastInput = useRef<CoachStreamInput | undefined>(undefined);
  const [scene, setScene] = useState<CoachScene>('GENERAL_CHAT');
  const [message, setMessage] = useState('');
  const [answer, setAnswer] = useState('');
  const [status, setStatus] = useState<'idle' | 'streaming' | 'completed' | 'cancelled' | 'error'>('idle');
  const [streamError, setStreamError] = useState<Extract<CoachStreamEvent, { type: 'error' }> | null>(null);

  useEffect(() => () => abort.current?.abort(), []);

  async function send(input: CoachStreamInput) {
    abort.current?.abort();
    const controller = new AbortController();
    abort.current = controller;
    lastInput.current = input;
    setAnswer('');
    setStreamError(null);
    setStatus('streaming');
    let terminalError: Extract<CoachStreamEvent, { type: 'error' }> | undefined;
    try {
      await api.streamCoach(input, { onEvent(event) {
        if (event.type === 'token') setAnswer((current) => current + event.text);
        if (event.type === 'error') { terminalError = event; setStreamError(event); setStatus('error'); }
      } }, controller.signal);
      if (!terminalError) setStatus('completed');
    } catch (error) {
      if (controller.signal.aborted) { setStatus('cancelled'); return; }
      setStreamError({ type: 'error', code: isApiError(error) ? error.code : 'NETWORK_ERROR', message: isApiError(error) ? error.message : '暂时无法连接智能教练', retryable: true });
      setStatus('error');
    }
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    const trimmed = message.trim();
    if (!trimmed) return;
    void send({ conversationId: conversationId.current, scene, message: trimmed });
  }

  return <main className="workspace coach-page">
    <header className="page-heading">
      <div><p className="eyebrow">受控工具 · 流式响应</p><h1>智能教练</h1></div>
      <p>教练可以解释记录和提出反思问题；计算、安全判断和计划变更仍由服务端规则负责。</p>
    </header>
    <div className="coach-layout">
      <form className="coach-composer" onSubmit={submit}>
        <label className="field" htmlFor="coach-scene"><span>对话场景</span><select id="coach-scene" name="scene" aria-label="对话场景" value={scene} onChange={(event) => setScene(event.target.value as CoachScene)} disabled={status === 'streaming'}>{scenes.map((item) => <option value={item.value} key={item.value}>{item.label}</option>)}</select></label>
        <label className="field" htmlFor="coach-message"><span>你的问题</span><textarea id="coach-message" name="message" aria-label="你的问题" rows={7} maxLength={4000} value={message} onChange={(event) => setMessage(event.target.value)} placeholder="写下你想复盘的情况" disabled={status === 'streaming'} /></label>
        <div className="coach-actions">{status === 'streaming' ? <button className="button button-secondary" type="button" onClick={() => abort.current?.abort()}><CircleStop size={17} />停止生成</button> : <button className="button button-primary" type="submit" disabled={!message.trim()}><Send size={17} />发送消息</button>}</div>
      </form>
      <section className="coach-response" aria-live="polite" aria-busy={status === 'streaming'}>
        <header><Bot size={21} /><div><h2>教练回复</h2><span>{status === 'idle' ? '等待提问' : status === 'streaming' ? '正在生成' : status === 'completed' ? '回复完成' : status === 'cancelled' ? '已停止' : '响应失败'}</span></div></header>
        {answer ? <p className="coach-answer">{answer}</p> : <div className="coach-empty">{status === 'streaming' ? <><span className="spinner" /><span>正在等待第一个片段</span></> : <span>选择场景并发送问题后，回复会显示在这里。</span>}</div>}
        {streamError && <div className="notice notice-error" role="alert"><AlertTriangle size={17} /><span>{streamError.message}</span></div>}
        {streamError?.retryable && lastInput.current && <button className="button button-secondary coach-retry" type="button" onClick={() => void send(lastInput.current!)}><RotateCcw size={17} />重试上次消息</button>}
        {scene === 'HBTI_INTERPRETATION' && <p className="coach-boundary">HBTI 仅用于调整表达重点，不决定热量、安全或高风险运动建议。</p>}
      </section>
    </div>
  </main>;
}
