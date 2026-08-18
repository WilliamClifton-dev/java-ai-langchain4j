import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, CheckCircle2, Save } from 'lucide-react';
import { useEffect, useState, type FormEvent } from 'react';

import type { ProfileInput, ScreeningInput } from '../api/domain';
import { api, isApiError } from '../api/http';

const ACTIVITY_OPTIONS = [
  ['SEDENTARY', '久坐为主'],
  ['LIGHT', '轻度活动'],
  ['MODERATE', '中等活动'],
  ['VERY_ACTIVE', '高活动量'],
] as const;

const SCREENING_ITEMS: Array<{ key: keyof ScreeningInput; label: string; detail: string }> = [
  { key: 'pregnantOrBreastfeeding', label: '怀孕或哺乳期', detail: '当前处于怀孕或哺乳阶段' },
  { key: 'eatingDisorderHistory', label: '进食障碍相关经历', detail: '曾因进食行为接受支持，或对此感到担忧' },
  { key: 'medicalGuidanceRequired', label: '需要医疗指导', detail: '医生建议在专业监督下进行体重管理' },
  { key: 'weightAffectingMedication', label: '影响体重的药物', detail: '正在使用可能明显影响体重或食欲的药物' },
  { key: 'concerningSymptoms', label: '需要关注的症状', detail: '近期出现晕厥、胸痛或其他令人担忧的症状' },
];

const EMPTY_SCREENING: ScreeningInput = {
  pregnantOrBreastfeeding: false,
  eatingDisorderHistory: false,
  medicalGuidanceRequired: false,
  weightAffectingMedication: false,
  concerningSymptoms: false,
};

function defaultProfile(): ProfileInput {
  return {
    dateOfBirth: '',
    calculationSex: 'FEMALE',
    heightCm: 165,
    currentWeightKg: 65,
    targetWeightKg: 65,
    activityLevel: 'LIGHT',
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Hong_Kong',
  };
}

function errorText(error: unknown) {
  return isApiError(error) ? error.message : '暂时无法保存，请稍后重试';
}

export function ProfilePage() {
  const queryClient = useQueryClient();
  const profileQuery = useQuery({ queryKey: ['profile'], queryFn: api.getProfile });
  const profileMissing = isApiError(profileQuery.error, 'PROFILE_NOT_FOUND');
  const screeningQuery = useQuery({
    queryKey: ['screening', 'current'],
    queryFn: api.getCurrentScreening,
    enabled: Boolean(profileQuery.data),
  });
  const screeningMissing = isApiError(screeningQuery.error, 'PROFILE_NOT_FOUND');
  const [profile, setProfile] = useState<ProfileInput>(defaultProfile);
  const [answers, setAnswers] = useState<ScreeningInput>(EMPTY_SCREENING);
  const [noRiskConfirmed, setNoRiskConfirmed] = useState(false);
  const [screeningRequired, setScreeningRequired] = useState(false);
  const [screeningValidation, setScreeningValidation] = useState<string | null>(null);

  useEffect(() => {
    if (profileQuery.data) {
      const { userId: _userId, ...saved } = profileQuery.data;
      setProfile(saved);
    }
  }, [profileQuery.data]);

  useEffect(() => {
    const current = screeningQuery.data;
    if (!current) return;
    const reasons = new Set(current.reasonCodes ?? []);
    setAnswers({
      pregnantOrBreastfeeding: reasons.has('PREGNANCY_OR_BREASTFEEDING'),
      eatingDisorderHistory: reasons.has('EATING_DISORDER_SUPPORT'),
      medicalGuidanceRequired: reasons.has('MEDICAL_REVIEW'),
      weightAffectingMedication: reasons.has('WEIGHT_AFFECTING_MEDICATION'),
      concerningSymptoms: reasons.has('CONCERNING_SYMPTOMS'),
    });
    setNoRiskConfirmed(current.automaticPlanningAllowed);
  }, [screeningQuery.data]);

  const saveProfile = useMutation({
    mutationFn: api.saveProfile,
    onSuccess: async (saved) => {
      queryClient.setQueryData(['profile'], saved);
      setScreeningRequired(true);
      setAnswers(EMPTY_SCREENING);
      setNoRiskConfirmed(false);
      setScreeningValidation(null);
      await queryClient.invalidateQueries({ queryKey: ['screening', 'current'] });
    },
  });
  const saveScreening = useMutation({
    mutationFn: api.createScreening,
    onSuccess: (saved) => {
      queryClient.setQueryData(['screening', 'current'], saved);
      setScreeningRequired(false);
    },
  });

  function submitProfile(event: FormEvent) {
    event.preventDefault();
    saveProfile.mutate(profile);
  }

  function submitScreening(event: FormEvent) {
    event.preventDefault();
    const hasRiskSelection = Object.values(answers).some(Boolean);
    if (!noRiskConfirmed && !hasRiskSelection) {
      setScreeningValidation('请明确确认“以上情况均不符合我”，或选择至少一项需要关注的情况');
      return;
    }
    setScreeningValidation(null);
    saveScreening.mutate(answers);
  }

  const screening = screeningQuery.data;
  const screeningReady = Boolean(
    screening
      && typeof screening.status === 'string'
      && typeof screening.automaticPlanningAllowed === 'boolean'
      && typeof screening.guidance === 'string',
  );

  return (
    <main className="workspace workspace-wide">
      <header className="page-heading">
        <div>
          <p className="eyebrow">基础设置</p>
          <h1>个人档案与安全筛查</h1>
        </div>
        <p>计算只使用必要信息。更新身高、体重或目标后，需要重新完成安全筛查。</p>
      </header>

      {profileQuery.isLoading ? (
        <div className="inline-loading" aria-live="polite"><span className="spinner" />正在读取档案</div>
      ) : profileQuery.isError && !profileMissing ? (
        <div className="notice notice-error" role="alert">档案读取失败，请刷新页面重试</div>
      ) : (
        <form className="data-section" onSubmit={submitProfile}>
          <div className="section-heading">
            <span className="step-index">01</span>
            <div><h2>计算档案</h2><p>所有测量均使用公制单位。</p></div>
          </div>
            <div className="form-grid">
            <label className="field"><span>出生日期</span><input type="date" required value={profile.dateOfBirth} onChange={(e) => setProfile({ ...profile, dateOfBirth: e.target.value })} /></label>
            <label className="field"><span>计算用性别</span><select value={profile.calculationSex} onChange={(e) => setProfile({ ...profile, calculationSex: e.target.value as ProfileInput['calculationSex'] })}><option value="FEMALE">女性公式</option><option value="MALE">男性公式</option></select></label>
            <label className="field"><span>身高（cm）</span><input type="number" min="100" max="250" step="0.1" required value={profile.heightCm} onChange={(e) => setProfile({ ...profile, heightCm: e.target.valueAsNumber })} /></label>
            <label className="field"><span>当前体重（kg）</span><input type="number" min="30" max="350" step="0.1" required value={profile.currentWeightKg} onChange={(e) => setProfile({ ...profile, currentWeightKg: e.target.valueAsNumber })} /></label>
            <label className="field"><span>日常活动水平</span><select value={profile.activityLevel} onChange={(e) => setProfile({ ...profile, activityLevel: e.target.value as ProfileInput['activityLevel'] })}>{ACTIVITY_OPTIONS.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label>
            <label className="field field-span"><span>时区</span><input type="text" maxLength={64} required value={profile.timeZone} onChange={(e) => setProfile({ ...profile, timeZone: e.target.value })} /></label>
          </div>
          {saveProfile.isError && <div className="notice notice-error" role="alert">{errorText(saveProfile.error)}</div>}
          {saveProfile.isSuccess && <div className="notice notice-success" role="status"><CheckCircle2 size={17} />档案已保存</div>}
          <div className="section-actions"><button className="button button-primary" disabled={saveProfile.isPending}><Save size={17} />{saveProfile.isPending ? '正在保存' : '保存档案'}</button></div>
        </form>
      )}

      {profileQuery.data && (
        <form className="data-section" onSubmit={submitScreening}>
            <div className="section-heading">
              <span className="step-index">02</span>
              <div><h2>安全筛查</h2><p>如实选择；风险项只用于决定是否暂停自动计划。减脂、维持或增肌方向会在计划页选择。</p></div>
            </div>
            <p className="screening-helper">请明确确认“以上情况均不符合我”，或选择至少一项需要关注的情况</p>
            <div className="screening-list">
            {SCREENING_ITEMS.map((item) => (
              <label className="screening-row" key={item.key}>
                <span><strong>{item.label}</strong><small>{item.detail}</small></span>
                <input type="checkbox" checked={answers[item.key]} onChange={(e) => { setAnswers({ ...answers, [item.key]: e.target.checked }); setNoRiskConfirmed(false); setScreeningValidation(null); }} />
              </label>
            ))}
            <label className="screening-row screening-none-row">
              <span><strong>以上情况均不符合我</strong><small>我目前没有上述情况，且没有需要补充说明的安全风险。</small></span>
              <input type="checkbox" aria-label="以上情况均不符合我" checked={noRiskConfirmed} onChange={(e) => { setNoRiskConfirmed(e.target.checked); if (e.target.checked) setAnswers(EMPTY_SCREENING); setScreeningValidation(null); }} />
            </label>
            </div>
          {screeningRequired && (
            <div className="notice notice-warning" role="status"><AlertTriangle size={17} />档案已更新，请提交新的安全筛查</div>
          )}
          {screeningReady && screening && !screeningRequired && (
            <div className={`screening-result screening-${screening.status.toLowerCase()}`} role="status">
              {screening.automaticPlanningAllowed ? <CheckCircle2 size={20} /> : <AlertTriangle size={20} />}
              <div><strong>{screening.automaticPlanningAllowed ? '可以进入自动计划' : '自动计划已暂停'}</strong><span>{screening.guidance}</span></div>
            </div>
          )}
          {screening && !screeningReady && !screeningQuery.isError && (
            <div className="notice notice-error" role="alert">安全状态数据格式异常，请刷新页面重试</div>
          )}
          {screeningQuery.isError && !screeningMissing && <div className="notice notice-error" role="alert">安全状态读取失败</div>}
          {saveScreening.isError && <div className="notice notice-error" role="alert">{errorText(saveScreening.error)}</div>}
          {screeningValidation && <div className="notice notice-warning" role="alert"><AlertTriangle size={17} />{screeningValidation}</div>}
          <div className="section-actions"><button className="button button-primary" disabled={saveScreening.isPending || (!noRiskConfirmed && !Object.values(answers).some(Boolean))}>{saveScreening.isPending ? '正在提交' : screening ? '重新提交筛查' : '提交筛查'}</button></div>
        </form>
      )}
    </main>
  );
}
