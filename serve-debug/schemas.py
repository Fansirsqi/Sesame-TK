from datetime import datetime
import json # Import json for parsing
from pydantic import BaseModel, field_validator, ValidationError # Import field_validator
from typing import Optional, Any # 导入 Optional 和 Any 用于可选字段和任意类型

from models import HookData # 保留，可能未来需要
from config import logger

# 定义 Pydantic 模型用于请求体和响应体
class HookDataBase(BaseModel):
    # 更新字段类型以匹配实际传入的数据
    TimeStamp: Optional[int] = None # 时间戳通常是整数
    Method: str
    Params: Optional[Any] = None # Params 可以是任意 JSON 对象/字典
    Data: Optional[Any] = None   # Data 可以是任意 JSON 对象/字典

class HookDataCreate(HookDataBase):
    pass # 创建时不需要 id, created_at, updated_at

class HookDataSchema(HookDataBase):
    id: int
    created_at: datetime
    updated_at: datetime

    # Override types from HookDataBase for response serialization
    TimeStamp: Optional[int] = None
    Params: Optional[Any] = None
    Data: Optional[Any] = None

    @field_validator('TimeStamp', mode='before')
    @classmethod
    def parse_timestamp(cls, value):
        if isinstance(value, str):
            try:
                return int(value)
            except (ValueError, TypeError):
                logger.warning(f"Could not parse timestamp string: {value}")
                return None # Or handle error as needed
        return value # Keep original if already int or None

    @field_validator('Params', 'Data', mode='before')
    @classmethod
    def parse_json_string(cls, value):
        if isinstance(value, str):
            try:
                return json.loads(value)
            except json.JSONDecodeError:
                logger.warning(f"Could not parse JSON string: {value}")
                # Decide how to handle invalid JSON: return original string, None, or raise error
                return value # Return original string if parsing fails
        return value # Keep original if already dict or None

    class Config:
        from_attributes = True # 允许从 ORM 对象创建 Pydantic 模型 (Pydantic V2)
