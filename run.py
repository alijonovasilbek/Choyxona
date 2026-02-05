from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse
from starlette.responses import JSONResponse

# Routers
# from routers.auth import router as auth_router

# --------------------------------------------------
# FASTAPI APP CONFIG
# --------------------------------------------------
app = FastAPI(
    title="CIMS Table-Based Auth API",
    version="1.0.0",
    description="Table-based SQLAlchemy bilan Auth Sistema",
)


# --------------------------------------------------
# CORS (handled only here, not in nginx)
# --------------------------------------------------
origins = [

]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# --------------------------------------------------
# ROUTERS
# --------------------------------------------------
# app.include_router(auth_router)




@app.get("/")
async def root():
    return {
        "message": "🚀 CIMS Table-Based Auth API",
        "approach": "Table-based SQLAlchemy",
        "docs": "/docs",
    }


# --------------------------------------------------
# ENTRYPOINT
# --------------------------------------------------
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("run:app", host="0.0.0.0", port=8000, reload=True)