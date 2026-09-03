#!/usr/bin/env python3
"""
Uso:
  1. Guarda el JSON de tu resultado del /dataset-ajustado/solve en resultado.json
     (en esta misma carpeta)
  2. Corre:  python3 armar_verificar.py
     Este script trae el dataset base via GET /dataset-ajustado, le inyecta
     tu resultado como sesionesActuales, lo manda a POST /verificar, e
     imprime el detalle EXACTO de que restriccion(es) fallan y cuantas veces.

Requiere: pip install requests
"""
import json
import requests

BASE = "http://localhost:8080/api/timetable"

# 1. Traer el dataset base (profesores/cursos/salas/ramos/config), SIN sesionesActuales
request = requests.get(f"{BASE}/dataset-ajustado").json()

# 2. Cargar el resultado ya resuelto (guardalo como resultado.json en esta misma carpeta)
with open("resultado.json", encoding="utf-8") as f:
    resultado = json.load(f)

# 3. Agrupar las sesiones del resultado por ramoId, en orden de indiceSesion
por_ramo = {}
for s in resultado["sesiones"]:
    por_ramo.setdefault(s["ramoId"], {})[s["indiceSesion"]] = {
        "dia": s["dia"], "bloque": s["bloque"], "salaId": s["salaId"]
    }

# 4. Inyectar sesionesActuales en cada ramo del request
for ramo in request["ramos"]:
    sesiones_de_este_ramo = por_ramo.get(ramo["id"], {})
    ramo["sesionesActuales"] = [
        sesiones_de_este_ramo[i] for i in sorted(sesiones_de_este_ramo)
    ]

# 5. Llamar a /verificar
resp = requests.post(f"{BASE}/verificar", json=request)
resp.raise_for_status()
data = resp.json()

print("Score:", data["score"], "| Factible:", data["factible"])
print()
print("Detalle por restriccion:")
for d in data.get("detalle", []):
    print(f"  - {d['restriccion']}: {d['score']}  ({d['ocurrencias']} ocurrencia(s))")
