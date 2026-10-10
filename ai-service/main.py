
import logging
import shutil
import subprocess
from contextlib import asynccontextmanager
from pathlib import Path
from urllib.parse import urlparse

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from rag_pipeline import (
    index_repository,
    ask_repository,
    load_indexed_repository,
)

# --------------------------------------------------
# CONFIGURATION
# --------------------------------------------------

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

BASE_DIR = Path(__file__).resolve().parent
STORAGE_DIR = BASE_DIR / "chroma_db"
STORAGE_DIR.mkdir(parents=True, exist_ok=True)

REPOSITORIES_DIR = Path("/tmp/repositories")
repository_stores = {}


# --------------------------------------------------
# REQUEST MODELS
# --------------------------------------------------

class IndexRequest(BaseModel):
    repository_id: str = Field(min_length=1, max_length=100)
    repository_url: str


class AskRequest(BaseModel):
    repository_id: str = Field(min_length=1, max_length=100)
    question: str = Field(min_length=1, max_length=2000)


# --------------------------------------------------
# LOAD EXISTING INDEXES
# --------------------------------------------------

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting AI Codebase Doctor")

    for index_path in STORAGE_DIR.iterdir():
        if not index_path.is_dir():
            continue

        repository_id = index_path.name

        try:
            vector_store = load_indexed_repository(
                repository_id=repository_id,
                persist_directory=str(index_path),
            )

            if vector_store is not None:
                repository_stores[repository_id] = vector_store
                logger.info(
                    "Loaded index for repository %s",
                    repository_id,
                )

        except Exception:
            logger.exception(
                "Failed to load repository index %s",
                repository_id,
            )

    yield

    repository_stores.clear()
    logger.info("Stopping AI Codebase Doctor")


# --------------------------------------------------
# FASTAPI APP
# --------------------------------------------------

app = FastAPI(
    title="AI Codebase Doctor",
    description=(
        "API for indexing GitHub repositories and asking questions "
        "about source code using Retrieval-Augmented Generation (RAG)."
    ),
    version="1.0.0",
    lifespan=lifespan,
    swagger_ui_parameters={
        "docExpansion": "list",
        "displayRequestDuration": True,
        "filter": True,
    },
)


# --------------------------------------------------
# ROOT AND HEALTH
# --------------------------------------------------

@app.get("/", tags=["System"], summary="Welcome")
async def root():
    return {
        "message": "Welcome to AI Codebase Doctor",
        "docs": "/docs",
        "health": "/health",
    }


@app.get("/health", tags=["System"], summary="Health Check")
async def health():
    return {
        "status": "healthy",
        "service": "AI Codebase Doctor",
        "loaded_repositories": len(repository_stores),
    }


# --------------------------------------------------
# INDEX REPOSITORY
# --------------------------------------------------

@app.post(
    "/api/index",
    tags=["Repository"],
    summary="Index Repository",
)
async def index_codebase(request: IndexRequest):
    repository_id = request.repository_id
    repository_url = request.repository_url.strip()

    # Accept only public GitHub HTTPS repository URLs.
    parsed_url = urlparse(repository_url)

    if (
            parsed_url.scheme != "https"
            or parsed_url.hostname != "github.com"
            or not parsed_url.path.strip("/")
            or parsed_url.query
            or parsed_url.fragment
            or parsed_url.username
            or parsed_url.password
    ):
        raise HTTPException(
            status_code=400,
            detail="Provide a valid HTTPS GitHub repository URL.",
        )

    repository_path = REPOSITORIES_DIR / repository_id
    persist_directory = STORAGE_DIR / repository_id

    # Avoid deleting an existing index or clone accidentally.
    if repository_id in repository_stores or persist_directory.exists():
        raise HTTPException(
            status_code=409,
            detail=(
                "An index already exists for this repository ID. "
                "Use a new repository ID to index it again."
            ),
        )

    try:
        REPOSITORIES_DIR.mkdir(parents=True, exist_ok=True)

        subprocess.run(
            [
                "git",
                "clone",
                "--depth",
                "1",
                repository_url,
                str(repository_path),
            ],
            check=True,
            capture_output=True,
            text=True,
            timeout=120,
        )

        result = index_repository(
            repository_path=str(repository_path),
            persist_directory=str(persist_directory),
            repository_id=repository_id,
        )

        # index_repository returns (vector_store, stats).
        if isinstance(result, tuple):
            vector_store, stats = result
        elif isinstance(result, dict):
            vector_store = result.get("vector_store")
            stats = result.get("stats", {})
        else:
            vector_store = result
            stats = {}

        if vector_store is None:
            raise RuntimeError(
                "Indexing completed without returning a vector store."
            )

        repository_stores[repository_id] = vector_store

        logger.info(
            "Repository %s indexed successfully",
            repository_id,
        )

        return {
            "message": "Repository indexed successfully",
            "repository_id": repository_id,
            "stats": stats,
        }

    except subprocess.CalledProcessError as exc:
        logger.error("Git clone failed: %s", exc.stderr)

        raise HTTPException(
            status_code=400,
            detail=(
                "Could not clone the repository. "
                "Check that the URL is valid and the repository is public."
            ),
        ) from exc

    except subprocess.TimeoutExpired as exc:
        raise HTTPException(
            status_code=408,
            detail="Repository cloning timed out.",
        ) from exc

    except HTTPException:
        raise

    except Exception as exc:
        logger.exception("Repository indexing failed")

        # Remove partial index data so a retry can work.
        repository_stores.pop(repository_id, None)
        shutil.rmtree(persist_directory, ignore_errors=True)

        raise HTTPException(
            status_code=500,
            detail="Repository indexing failed. Check AI service logs.",
        ) from exc


# --------------------------------------------------
# ASK QUESTIONS
# --------------------------------------------------

@app.post(
    "/api/ask",
    tags=["Codebase Questions"],
    summary="Ask Your Codebase",
)
async def ask_codebase(request: AskRequest):
    repository_id = request.repository_id
    vector_store = repository_stores.get(repository_id)

    if vector_store is None:
        raise HTTPException(
            status_code=404,
            detail="Repository index not found. Index the repository first.",
        )

    try:
        result = ask_repository(
            question=request.question,
            vector_store=vector_store,
        )

        if isinstance(result, dict):
            return {
                "repository_id": repository_id,
                **result,
            }

        return {
            "repository_id": repository_id,
            "answer": str(result),
            "sources": [],
        }

    except HTTPException:
        raise

    except Exception as exc:
        logger.exception("Question answering failed")

        raise HTTPException(
            status_code=500,
            detail=f"Question answering failed: {exc}",
        ) from exc
