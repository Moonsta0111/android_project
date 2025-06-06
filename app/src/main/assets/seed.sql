-- 최저시급 데이터 (2024-2025년 기준)
INSERT INTO MinimumWage (year, hourlyWage) VALUES 
(2024, 9860),
(2025, 10030);

-- 기본 관리자 계정 (비밀번호: admin1234)
INSERT INTO Employee (username, password, name, hourlyWage, role)
VALUES (
    'admin',
    'admin1234',
    '관리자',
    0,
    'OWNER'
);

-- 샘플 직원 데이터 (비밀번호: worker123)
INSERT INTO Employee (username, password, name, hourlyWage, role)
VALUES 
(
    'worker1',
    'worker123',
    '홍길동',
    10030,
    'WORKER'
),
(
    'worker2',
    'worker123',
    '김철수',
    11000,
    'WORKER'
),
(
    'worker3',
    'worker123',
    '이영희',
    10500,
    'WORKER'
);

-- 샘플 근무 기록 (2024년 1월)
INSERT INTO Shift (employeeId, startTime, endTime)
VALUES 
(2, '2024-01-01 09:00:00', '2024-01-01 18:00:00'),
(2, '2024-01-02 09:00:00', '2024-01-02 18:00:00'),
(3, '2024-01-01 13:00:00', '2024-01-01 22:00:00'),
(3, '2024-01-02 13:00:00', '2024-01-02 22:00:00'),
(4, '2024-01-01 10:00:00', '2024-01-01 19:00:00'),
(4, '2024-01-02 10:00:00', '2024-01-02 19:00:00'); 