from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from database import database

# Import routers
from routers.auth_router import router as auth_router
from routers.users_router import router as users_router
from routers.rooms_router import router as rooms_router
from routers.bookings_router import router as bookings_router
from routers.reports_router import router as reports_router
from routers.notifications_router import router as notifications_router
from  routers.local_notification_router import router as local_notification_router

# Import scheduler
from services.scheduler import scheduler

# --------------------------------------------------
# FASTAPI APP CONFIG
# --------------------------------------------------
app = FastAPI(
    title="Choyxona Bron Tizimi API",
    version="1.0.0",
    description="Choyxona uchun bron tizimi - Xonalar, Bronlar va Hisobotlar",
    redirect_slashes=False
)


# --------------------------------------------------
# DATABASE CONNECTION & SCHEDULER
# --------------------------------------------------
@app.on_event("startup")
async def startup():
    """Connect to database and start scheduler on startup"""
    await database.connect()
    print("✅ Database connected")

    # Start notification scheduler
    scheduler.start()
    print("✅ Notification scheduler started")


@app.on_event("shutdown")
async def shutdown():
    """Disconnect from database and stop scheduler on shutdown"""
    scheduler.shutdown()
    print("❌ Notification scheduler stopped")

    await database.disconnect()
    print("❌ Database disconnected")


# --------------------------------------------------
# CORS
# --------------------------------------------------
origins = [
   "*"
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["*"]
)

# --------------------------------------------------
# ROUTERS
# --------------------------------------------------
app.include_router(auth_router)
app.include_router(users_router)
app.include_router(rooms_router)
app.include_router(bookings_router)
app.include_router(reports_router)
# app.include_router(notifications_router)
app.include_router(local_notification_router)


@app.get("/")
async def root():
    """API Root endpoint"""
    return {
        "message": "🍵 Choyxona Bron Tizimi API",
        "version": "1.0.0",
        "docs": "/docs",
        "features": [
            "User Authentication (JWT)",
            "User Management (Superadmin, Admin, Oshpaz)",
            "Room/Place Management",
            "Booking System",
            "Reports & Analytics"
        ]
    }


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "database": "connected"}


# --------------------------------------------------
# ENTRYPOINT
# --------------------------------------------------
if __name__ == "__main__":
    import uvicorn

    uvicorn.run("run:app", host="0.0.0.0", port=8000, reload=True)
