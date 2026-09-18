CREATE TABLE assessment_definition (
    id VARCHAR(36) NOT NULL,
    assessment_key VARCHAR(64) NOT NULL,
    version VARCHAR(32) NOT NULL,
    scoring_rule_version VARCHAR(32) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    answer_min TINYINT NOT NULL,
    answer_max TINYINT NOT NULL,
    source_repository VARCHAR(255) NOT NULL,
    source_commit CHAR(40) NOT NULL,
    source_content_hash CHAR(64) NOT NULL,
    published_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_assessment_definition_version UNIQUE (assessment_key, version),
    CONSTRAINT ck_assessment_definition_status CHECK (status IN ('PUBLISHED', 'RETIRED')),
    CONSTRAINT ck_assessment_definition_answer_range CHECK (
        answer_min >= 1 AND answer_max > answer_min
    )
);

CREATE TABLE assessment_dimension (
    definition_id VARCHAR(36) NOT NULL,
    dimension_code VARCHAR(8) NOT NULL,
    ordinal TINYINT NOT NULL,
    left_pole CHAR(1) NOT NULL,
    right_pole CHAR(1) NOT NULL,
    left_label VARCHAR(64) NOT NULL,
    right_label VARCHAR(64) NOT NULL,
    description_zh VARCHAR(255) NOT NULL,
    description_en VARCHAR(255) NOT NULL,
    PRIMARY KEY (definition_id, dimension_code),
    CONSTRAINT uk_assessment_dimension_ordinal UNIQUE (definition_id, ordinal),
    CONSTRAINT fk_assessment_dimension_definition
        FOREIGN KEY (definition_id) REFERENCES assessment_definition (id)
        ON DELETE RESTRICT
);

CREATE TABLE assessment_item (
    id VARCHAR(64) NOT NULL,
    definition_id VARCHAR(36) NOT NULL,
    item_key VARCHAR(16) NOT NULL,
    ordinal SMALLINT NOT NULL,
    dimension_code VARCHAR(8) NOT NULL,
    target_pole CHAR(1) NOT NULL,
    title_zh VARCHAR(500) NOT NULL,
    hint_zh VARCHAR(255) NOT NULL,
    title_en VARCHAR(500) NOT NULL,
    hint_en VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_assessment_item_key UNIQUE (definition_id, item_key),
    CONSTRAINT uk_assessment_item_ordinal UNIQUE (definition_id, ordinal),
    CONSTRAINT fk_assessment_item_definition
        FOREIGN KEY (definition_id) REFERENCES assessment_definition (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_assessment_item_dimension
        FOREIGN KEY (definition_id, dimension_code)
        REFERENCES assessment_dimension (definition_id, dimension_code)
        ON DELETE RESTRICT
);

INSERT INTO assessment_definition (
    id, assessment_key, version, scoring_rule_version, display_name, status,
    answer_min, answer_max, source_repository, source_commit,
    source_content_hash, published_at
) VALUES (
    '00000000-0000-4000-8000-000000000001', 'hbti', '1.0.0', '1.0.0',
    'HBTI Exploratory Behavioral Tendency Assessment', 'PUBLISHED', 1, 5,
    'https://github.com/WilliamClifton-dev/hbti-prototype',
    'bdd1e9fbd75ae9ebdb869d42c61ae7c82cafc76e',
    'dee8280f3e7ae2ead7786413987c0c0bc07eadf07b6ff85e0c73034084de531a',
    '2026-08-14 00:00:00.000000'
);

INSERT INTO assessment_dimension (
    definition_id, dimension_code, ordinal, left_pole, right_pole,
    left_label, right_label, description_zh, description_en
) VALUES
('00000000-0000-4000-8000-000000000001', 'FS', 1, 'F', 'S', 'Fuel Flexible', 'Sugar Sensitive', '反映你对空腹、碳水波动与燃料切换的耐受差异。', 'How you tolerate fasting, carbohydrate fluctuations, and shifts between fuel sources.'),
('00000000-0000-4000-8000-000000000001', 'HC', 2, 'H', 'C', 'Hunger Stable', 'Craving Reactive', '反映你的饥饿节律更接近生理驱动，还是更受奖励与情绪驱动。', 'Whether eating is guided more by physical hunger or by reward and emotional cues.'),
('00000000-0000-4000-8000-000000000001', 'RW', 3, 'R', 'W', 'Recovery Efficient', 'Stress Wired', '反映睡眠、压力与恢复对你代谢状态的影响强弱。', 'How strongly sleep, stress, and recovery affect your metabolic and behavioral state.'),
('00000000-0000-4000-8000-000000000001', 'ND', 4, 'N', 'D', 'Naturally Active', 'Energy Defending', '反映你是更容易维持高 NEAT，还是更容易进入节能防御。', 'Whether you tend to maintain spontaneous activity or reduce movement when energy is limited.');

INSERT INTO assessment_item (
    id, definition_id, item_key, ordinal, dimension_code, target_pole,
    title_zh, hint_zh, title_en, hint_en
) VALUES
('hbti-1.0.0-q1', '00000000-0000-4000-8000-000000000001', 'q1', 1, 'FS', 'F', '两餐之间，我通常能稳定维持精力，不太需要频繁加餐。', '对应燃料灵活性与血糖波动耐受', 'Between meals, my energy usually stays steady without frequent snacks.', 'Fuel flexibility and tolerance of glucose fluctuations'),
('hbti-1.0.0-q2', '00000000-0000-4000-8000-000000000001', 'q2', 2, 'FS', 'S', '高碳水一餐后，我很容易犯困、饿得快，或者注意力明显下滑。', '对应餐后波动敏感性', 'After a high-carbohydrate meal, I often feel sleepy, hungry again quickly, or less focused.', 'Sensitivity to post-meal fluctuations'),
('hbti-1.0.0-q3', '00000000-0000-4000-8000-000000000001', 'q3', 3, 'FS', 'F', '如果偶尔延后吃饭，我的状态通常还算稳定，不会立刻变得很难受。', '对应空腹适应能力', 'If a meal is delayed, I can usually remain functional without feeling unwell right away.', 'Adaptation to short periods without food'),
('hbti-1.0.0-q4', '00000000-0000-4000-8000-000000000001', 'q4', 4, 'FS', 'S', '我对甜食、精制主食或奶茶这类高奖励碳水的反应特别强。', '对应糖奖励与耐受差异', 'I react strongly to sweets, refined starches, or other highly rewarding carbohydrate foods.', 'Carbohydrate reward response and tolerance'),
('hbti-1.0.0-q5', '00000000-0000-4000-8000-000000000001', 'q5', 5, 'HC', 'H', '我的饥饿感比较规律，接近生理信号，而不是突然被某种食物勾起来。', '对应饥饿稳定性', 'My hunger follows a fairly regular physical rhythm rather than appearing suddenly when I see certain foods.', 'Stability of hunger signals'),
('hbti-1.0.0-q6', '00000000-0000-4000-8000-000000000001', 'q6', 6, 'HC', 'C', '压力、无聊或情绪波动时，我明显更想吃高热量食物。', '对应情绪性进食倾向', 'Stress, boredom, or emotional changes make me want high-calorie foods more strongly.', 'Tendency toward emotional eating'),
('hbti-1.0.0-q7', '00000000-0000-4000-8000-000000000001', 'q7', 7, 'HC', 'H', '只要一餐蛋白质和纤维够，我通常能比较自然地停下来，不容易失控。', '对应饱腹反馈', 'When a meal contains enough protein and fiber, I can usually stop eating without feeling out of control.', 'Satiety feedback'),
('hbti-1.0.0-q8', '00000000-0000-4000-8000-000000000001', 'q8', 8, 'HC', 'C', '我经常不是因为饿，而是因为想吃某个味道、口感或零食而进食。', '对应奖励驱动', 'I often eat for a specific taste, texture, or snack even when I am not physically hungry.', 'Reward-driven eating'),
('hbti-1.0.0-q9', '00000000-0000-4000-8000-000000000001', 'q9', 9, 'RW', 'R', '睡好一晚后，我的训练恢复、精神和食欲通常都会明显改善。', '对应恢复响应', 'After a good night''s sleep, my training recovery, energy, and appetite usually improve noticeably.', 'Recovery response'),
('hbti-1.0.0-q10', '00000000-0000-4000-8000-000000000001', 'q10', 10, 'RW', 'W', '一旦连续几天睡眠不足，我就更容易暴食、浮肿或体重波动。', '对应压力脆弱性', 'After several nights of insufficient sleep, I am more likely to overeat, retain fluid, or see weight fluctuations.', 'Vulnerability to insufficient recovery'),
('hbti-1.0.0-q11', '00000000-0000-4000-8000-000000000001', 'q11', 11, 'RW', 'R', '我通常能承受适量训练，不太会因为轻中度运动就恢复很久。', '对应恢复效率', 'I can usually tolerate moderate exercise without needing unusually long recovery afterward.', 'Recovery efficiency'),
('hbti-1.0.0-q12', '00000000-0000-4000-8000-000000000001', 'q12', 12, 'RW', 'W', '高压阶段时，我的食欲、能量和作息很容易一起失控。', '对应压力耦合', 'During high-stress periods, my appetite, energy, and daily schedule tend to become unstable together.', 'Coupling between stress and daily regulation'),
('hbti-1.0.0-q13', '00000000-0000-4000-8000-000000000001', 'q13', 13, 'ND', 'N', '我平时会自然地多走动、换姿势、站起来或者做零碎活动。', '对应 NEAT 倾向', 'I naturally walk around, change position, stand up, or add small amounts of movement during the day.', 'Non-exercise activity tendency'),
('hbti-1.0.0-q14', '00000000-0000-4000-8000-000000000001', 'q14', 14, 'ND', 'D', '一旦吃少一点或训练多一点，我会很快变懒、变冷或不想动。', '对应节能防御', 'When I eat less or train more, I quickly feel less active, colder, or unwilling to move.', 'Energy-conservation response'),
('hbti-1.0.0-q15', '00000000-0000-4000-8000-000000000001', 'q15', 15, 'ND', 'N', '就算没有专门运动，我每天的总活动量通常也不算低。', '对应自发活动水平', 'Even without planned exercise, my total daily activity is usually not low.', 'Spontaneous activity level'),
('hbti-1.0.0-q16', '00000000-0000-4000-8000-000000000001', 'q16', 16, 'ND', 'D', '节食时，我的身体很像会主动省电，日常活动欲望明显下降。', '对应适应性代谢节能', 'While dieting, my body seems to conserve energy and my desire for everyday movement drops noticeably.', 'Adaptive energy conservation');
