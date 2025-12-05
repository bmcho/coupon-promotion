    -- point 스키마 생성
    CREATE DATABASE IF NOT EXISTS point
      CHARACTER SET utf8mb4
      COLLATE utf8mb4_unicode_ci;

    -- point 전용 유저 생성
    CREATE USER IF NOT EXISTS 'point-user'@'%' IDENTIFIED BY 'point1234!';

    -- point 스키마 권한 부여
    GRANT ALL PRIVILEGES ON point.* TO 'point-user'@'%';

    FLUSH PRIVILEGES;