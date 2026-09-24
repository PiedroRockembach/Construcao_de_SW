#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "=========================================================="
echo "    Executando Testes de Mutação com Pitest"
echo "=========================================================="

MODULE="${1:-all}"

if [ "$MODULE" == "all" ]; then
    echo "Executando Pitest em todos os servicos (pecas, clientes, representantes)..."
    mvn test-compile org.pitest:pitest-maven:mutationCoverage -pl pecas-service,clientes-service,representantes-service
else
    echo "Executando Pitest no modulo: $MODULE..."
    mvn test-compile org.pitest:pitest-maven:mutationCoverage -pl "$MODULE"
fi

echo ""
echo "=========================================================="
echo "    Relatórios de Mutação Gerados com Sucesso!"
echo "=========================================================="
echo "pecas-service:          file://$DIR/pecas-service/target/pit-reports/index.html"
echo "clientes-service:       file://$DIR/clientes-service/target/pit-reports/index.html"
echo "representantes-service: file://$DIR/representantes-service/target/pit-reports/index.html"
echo "=========================================================="
