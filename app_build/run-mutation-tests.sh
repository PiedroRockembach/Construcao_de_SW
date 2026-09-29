#!/usr/bin/env bash
set -e

# Localizar executavel do Maven
if command -v mvn >/dev/null 2>&1; then
    MVN_CMD="mvn"
elif [ -f "/c/Users/eduep/.maven/maven-3.9.15/bin/mvn" ]; then
    MVN_CMD="/c/Users/eduep/.maven/maven-3.9.15/bin/mvn"
else
    MVN_CMD="mvn"
fi

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "======================================================================"
echo " Execucao de Testes de Mutacao com PITest (Ciclo 3)"
echo "======================================================================"

run_pitest() {
    local SERVICE=$1
    echo ""
    echo "----------------------------------------------------------------------"
    echo " Rodando PITest no servico: ${SERVICE}"
    echo "----------------------------------------------------------------------"
    cd "${BASE_DIR}/${SERVICE}"
    ${MVN_CMD} test-compile pitest:mutationCoverage
    cd "${BASE_DIR}"
}

TARGET=$1

if [ -z "$TARGET" ]; then
    echo "Executando testes de mutacao em todos os microsservicos de negocio..."
    run_pitest "pecas-service"
    run_pitest "clientes-service"
    run_pitest "representantes-service"
else
    run_pitest "$TARGET"
fi

echo ""
echo "======================================================================"
echo " Relatorios de Mutacao gerados:"
echo "  - pecas-service:          pecas-service/target/pit-reports/index.html"
echo "  - clientes-service:       clientes-service/target/pit-reports/index.html"
echo "  - representantes-service: representantes-service/target/pit-reports/index.html"
echo "======================================================================"
