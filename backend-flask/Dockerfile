FROM python:3.14-slim
WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY main.py ./
USER nobody
EXPOSE 8080
# One worker process keeps the in-memory store in one place; threads handle concurrency.
CMD ["gunicorn", "--bind", "0.0.0.0:8080", "--threads", "4", "main:app"]
