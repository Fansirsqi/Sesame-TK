# 新增配置文件 config.py
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from contextlib import contextmanager
import os
import logging


SQLALCHEMY_DATABASE_URL = "sqlite:///./webhook.db"
# 生产环境建议使用连接池
# SQLALCHEMY_DATABASE_URL = "postgresql://user:password@localhost/dbname"

# 配置日志
logging.basicConfig(
    level=logging.DEBUG ,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)

logger = logging.getLogger(__name__)


engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False}  # SQLite特有配置
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

# 数据库会话管理器
@contextmanager
def db_session():
    session = SessionLocal()
    try:
        yield session
    finally:
        session.close()
