# main.py - 完整改造后的API

from typing import List

# Corrected imports for exception handling
import json  # Import the json module for serialization
from fastapi import Depends, HTTPException, Query, Request, status, FastAPI
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from sqlalchemy.orm import Session
from datetime import datetime

from config import Base, db_session, engine, logger
from models import HookData as HookDataModel  # 重命名以避免冲突
from schemas import HookDataSchema, HookDataCreate  # 导入 Pydantic 模型


app = FastAPI()


# 添加自定义异常处理器来记录详细的验证错误
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    logger.error(f"Request validation failed: {exc.errors()}")  # 记录详细错误
    # 可以选择返回更详细的错误信息给客户端，或者保持默认行为
    # return JSONResponse(
    #     status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
    #     content={"detail": exc.errors()},
    # )
    # 这里我们先只记录日志，并返回FastAPI默认的422响应体结构
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={"detail": exc.errors()},  # 返回详细错误，方便调试
    )


# 初始化数据库
Base.metadata.create_all(bind=engine)


# 依赖注入
def get_db():
    with db_session() as session:
        yield session


# 恢复 response_model 为 HookDataSchema
@app.post("/hook", response_model=HookDataSchema)
async def create_webhook(data: HookDataCreate, db: Session = Depends(get_db)):
    try:
        # Prepare data for SQLAlchemy model, serializing dicts and converting types
        validated_data = data.model_dump(exclude_unset=True)
        params_data = validated_data.get("Params")
        data_field = validated_data.get("Data")
        timestamp_data = validated_data.get("TimeStamp")

        # Serialize Params if it's a dict
        if isinstance(params_data, dict):
            validated_data["Params"] = json.dumps(params_data, ensure_ascii=False)
        elif params_data is not None:  # Ensure it's at least a string if not None/dict
            validated_data["Params"] = str(params_data)

        # Serialize Data if it's a dict
        if isinstance(data_field, dict):
            validated_data["Data"] = json.dumps(data_field, ensure_ascii=False)
        elif data_field is not None:  # Ensure it's at least a string if not None/dict
            validated_data["Data"] = str(data_field)

        # Convert TimeStamp to string if it's an int
        if isinstance(timestamp_data, int):
            validated_data["TimeStamp"] = str(timestamp_data)
        elif timestamp_data is not None:  # Ensure it's at least a string if not None/int
            validated_data["TimeStamp"] = str(timestamp_data)

        # 使用 SQLAlchemy 模型创建数据库记录
        db_data = HookDataModel(**validated_data)
        db.add(db_data)
        db.commit()
        db.refresh(db_data)
        return db_data
    except Exception as e:
        logger.error(f"Error saving data: {e}")
        raise HTTPException(status_code=500, detail="Error saving data")


# main.py 中的 GET 接口完整代码
@app.get("/hook", response_model=List[HookDataSchema])  # 使用 Pydantic Schema 列表作为响应模型
async def get_webhooks(
    page: int = Query(1, ge=1),
    per_page: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
):
    # 自动清理旧数据（每小时执行）
    if datetime.now().minute == 0:
        HookDataModel.cleanup_old_data(db,max_count=10)  # 调用清理方法时使用 SQLAlchemy 模型

    skip = (page - 1) * per_page
    items = (
        db.query(HookDataModel)  # 查询时仍使用 SQLAlchemy 模型
        .offset(skip)
        .limit(per_page)
        .all()
    )



    return items  # 返回列表会被自动转换


if __name__ == "__main__":
    import uvicorn

    logger.info("Starting FastAPI server...")
    uvicorn.run(app, host="0.0.0.0", port=9527)
    logger.info("FastAPI server stopped.")
