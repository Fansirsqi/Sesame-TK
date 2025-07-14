# models.py - 数据库模型
from typing import Optional
from sqlalchemy import Column, Integer, String, DateTime, Text
from sqlalchemy.sql import func
from datetime import datetime, timedelta
from typing import Optional # Import Optional for type hints


from config import Base

class HookData(Base):
    __tablename__ = "hookdata"

    id = Column(Integer, primary_key=True, index=True)
    TimeStamp = Column(Text) #Column(DateTime, default=datetime.now())
    Method = Column(String(50))
    Params = Column(Text)
    Data = Column(Text)
    created_at = Column(DateTime, default=func.now())
    updated_at = Column(DateTime, default=func.now(), onupdate=func.now())

    # 自动清理策略（每小时触发）
    @classmethod
    def cleanup_old_data(cls, session, max_age_days: Optional[int] = 30, max_count: Optional[int] = None):
        """
        Cleans up old data based on maximum age and/or maximum count.

        Args:
            session: The database session.
            max_age_days: Maximum age in days to keep records. Records older than this will be deleted.
                          Set to None to disable age-based cleanup. Defaults to 30.
            max_count: Maximum number of records to keep. If the total count exceeds this,
                       the oldest records will be deleted until the count is met.
                       Set to None to disable count-based cleanup. Defaults to None.
        """
        from config import logger # Import logger locally if needed
        from sqlalchemy.sql import func # Ensure func is available
        from typing import Optional # Ensure Optional is available

        deleted_by_age_count = 0
        if max_age_days is not None and max_age_days > 0:
            try:
                cutoff_date = datetime.now() - timedelta(days=max_age_days)
                # Corrected column name from cls.timestamp to cls.created_at
                query_age = session.query(cls).filter(cls.created_at < cutoff_date)
                # Use synchronize_session=False for potentially large deletes
                deleted_by_age_count = query_age.delete(synchronize_session=False)
                logger.info(f"Deleted {deleted_by_age_count} records older than {max_age_days} days.")
            except Exception as e:
                logger.error(f"Error during age-based cleanup: {e}")
                session.rollback() # Rollback on error during age cleanup
                return # Stop further processing if age cleanup failed

        deleted_by_count_count = 0
        if max_count is not None and max_count > 0:
            try:
                total_count = session.query(func.count(cls.id)).scalar()
                to_delete_count = max(0, total_count - max_count)

                if to_delete_count > 0:
                    # Find the IDs of the oldest records to delete
                    oldest_ids_subquery = session.query(cls.id)\
                                                 .order_by(cls.created_at.asc())\
                                                 .limit(to_delete_count)\
                                                 .subquery()

                    # Delete the records with those IDs
                    query_count = session.query(cls).filter(cls.id.in_(oldest_ids_subquery))
                    deleted_by_count_count = query_count.delete(synchronize_session=False)
                    logger.info(f"Deleted {deleted_by_count_count} oldest records to meet max_count of {max_count}.")
            except Exception as e:
                logger.error(f"Error during count-based cleanup: {e}")
                session.rollback() # Rollback on error during count cleanup
                # Even if count cleanup fails, age cleanup might have succeeded, so commit that part if needed.
                # However, it's safer to rollback everything if any part fails.
                # If partial success is desired, commit needs careful placement.
                return # Stop further processing

        # Commit only if no errors occurred during cleanup steps
        if deleted_by_age_count > 0 or deleted_by_count_count > 0:
             try:
                 session.commit()
                 logger.info("Cleanup committed successfully.")
             except Exception as e:
                 logger.error(f"Error committing cleanup transaction: {e}")
                 session.rollback()
        else:
            logger.info("No records needed cleanup.")


