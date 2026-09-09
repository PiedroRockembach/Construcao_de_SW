// URL base através do próprio Gateway
const API_BASE = window.location.origin + "/api";

// Função para exibir alertas amigáveis
function showAlert(message, type = "success") {
    const container = document.getElementById("alertContainer");
    const alertId = "alert-" + Date.now();
    const alertHtml = `
        <div id="${alertId}" class="alert alert-${type} alert-dismissible fade show shadow-sm" role="alert">
            <i class="bi bi-${type === 'success' ? 'check-circle-fill' : type === 'warning' ? 'exclamation-triangle-fill' : 'x-circle-fill'} me-2"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
    container.innerHTML = alertHtml;
    setTimeout(() => {
        const el = document.getElementById(alertId);
        if (el) {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(el);
            bsAlert.close();
        }
    }, 4500);
}

// ======================== PEÇAS ========================
async function carregarPecas() {
    try {
        const response = await fetch(`${API_BASE}/pecas`);
        if (!response.ok) throw new Error(`Erro ao buscar peças: HTTP ${response.status}`);
        const pecas = await response.json();
        renderTabelaPecas(pecas);
    } catch (err) {
        console.error(err);
        showAlert(`Falha ao conectar ao serviço de peças via Gateway: ${err.message}`, "danger");
    }
}

function renderTabelaPecas(pecas) {
    const tbody = document.getElementById("tabelaPecasBody");
    if (!pecas || pecas.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">Nenhuma peça cadastrada.</td></tr>`;
        return;
    }
    tbody.innerHTML = pecas.map(p => `
        <tr>
            <td class="fw-bold">${p.id}</td>
            <td><span class="badge bg-primary-subtle text-primary border border-primary-subtle">${escapeHtml(p.numeroIdentificacao)}</span></td>
            <td class="fw-semibold">${escapeHtml(p.nome)}</td>
            <td class="text-secondary">${escapeHtml(p.descricao || "-")}</td>
        </tr>
    `).join("");
}

async function handleCadastrarPeca(event) {
    event.preventDefault();
    const numeroIdentificacao = document.getElementById("pecaNumero").value.trim();
    const nome = document.getElementById("pecaNome").value.trim();
    const descricao = document.getElementById("pecaDescricao").value.trim();

    try {
        const response = await fetch(`${API_BASE}/pecas`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ numeroIdentificacao, nome, descricao })
        });

        if (response.status === 201) {
            const nova = await response.json();
            showAlert(`Peça "${nova.nome}" cadastrada com sucesso (ID: ${nova.id})!`, "success");
            document.getElementById("formPeca").reset();
            carregarPecas();
        } else {
            const erro = await response.json().catch(() => ({}));
            showAlert(`Erro ao cadastrar peça: ${erro.message || response.statusText}`, "danger");
        }
    } catch (err) {
        showAlert(`Falha de comunicação com Gateway: ${err.message}`, "danger");
    }
}

async function buscarPecaPorNome() {
    const nome = document.getElementById("buscaPecaNome").value.trim();
    try {
        const response = await fetch(`${API_BASE}/pecas/busca?nome=${encodeURIComponent(nome)}`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const pecas = await response.json();
        renderTabelaPecas(pecas);
        showAlert(`Busca de peças por "${nome}": ${pecas.length} resultado(s) encontrado(s).`, "info");
    } catch (err) {
        showAlert(`Erro ao buscar peças por nome: ${err.message}`, "danger");
    }
}

async function buscarPecaPorId() {
    const id = document.getElementById("buscaPecaId").value.trim();
    if (!id) {
        carregarPecas();
        return;
    }
    try {
        const response = await fetch(`${API_BASE}/pecas/${id}`);
        if (response.status === 404) {
            showAlert(`Nenhuma peça encontrada com ID ${id}.`, "warning");
            renderTabelaPecas([]);
            return;
        }
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const peca = await response.json();
        renderTabelaPecas([peca]);
        showAlert(`Peça ID ${id} encontrada!`, "success");
    } catch (err) {
        showAlert(`Erro ao consultar peça por ID: ${err.message}`, "danger");
    }
}

// ======================== CLIENTES ========================
async function carregarClientes() {
    try {
        const response = await fetch(`${API_BASE}/clientes`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const clientes = await response.json();
        renderTabelaClientes(clientes);
    } catch (err) {
        console.error(err);
        showAlert(`Falha ao conectar ao serviço de clientes via Gateway: ${err.message}`, "danger");
    }
}

function renderTabelaClientes(clientes) {
    const tbody = document.getElementById("tabelaClientesBody");
    if (!clientes || clientes.length === 0) {
        tbody.innerHTML = `<tr><td colspan="3" class="text-center text-muted py-4">Nenhum cliente cadastrado.</td></tr>`;
        return;
    }
    tbody.innerHTML = clientes.map(c => `
        <tr>
            <td class="fw-bold">${c.id}</td>
            <td><span class="badge bg-success-subtle text-success border border-success-subtle">${escapeHtml(c.cpf)}</span></td>
            <td class="fw-semibold">${escapeHtml(c.nome)}</td>
        </tr>
    `).join("");
}

async function handleCadastrarCliente(event) {
    event.preventDefault();
    const cpf = document.getElementById("clienteCpf").value.trim();
    const nome = document.getElementById("clienteNome").value.trim();

    try {
        const response = await fetch(`${API_BASE}/clientes`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ cpf, nome })
        });

        if (response.status === 201) {
            const novo = await response.json();
            showAlert(`Cliente "${novo.nome}" cadastrado com sucesso!`, "success");
            document.getElementById("formCliente").reset();
            carregarClientes();
        } else {
            const erro = await response.json().catch(() => ({}));
            showAlert(`Erro ao cadastrar cliente: ${erro.message || response.statusText}`, "danger");
        }
    } catch (err) {
        showAlert(`Falha de comunicação com Gateway: ${err.message}`, "danger");
    }
}

async function buscarClientePorNome() {
    const nome = document.getElementById("buscaClienteNome").value.trim();
    try {
        const response = await fetch(`${API_BASE}/clientes/busca?nome=${encodeURIComponent(nome)}`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const clientes = await response.json();
        renderTabelaClientes(clientes);
        showAlert(`Busca de clientes por "${nome}": ${clientes.length} resultado(s).`, "info");
    } catch (err) {
        showAlert(`Erro ao buscar clientes: ${err.message}`, "danger");
    }
}

async function buscarClientePorCpf() {
    const cpf = document.getElementById("buscaClienteCpf").value.trim();
    if (!cpf) {
        carregarClientes();
        return;
    }
    try {
        const response = await fetch(`${API_BASE}/clientes/cpf/${encodeURIComponent(cpf)}`);
        if (response.status === 404) {
            showAlert(`Nenhum cliente encontrado com CPF ${cpf}.`, "warning");
            renderTabelaClientes([]);
            return;
        }
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const cliente = await response.json();
        renderTabelaClientes([cliente]);
        showAlert(`Cliente encontrado com CPF ${cpf}!`, "success");
    } catch (err) {
        showAlert(`Erro ao consultar cliente por CPF: ${err.message}`, "danger");
    }
}

// ======================== REPRESENTANTES ========================
async function carregarRepresentantes() {
    try {
        const response = await fetch(`${API_BASE}/representantes`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const reps = await response.json();
        renderTabelaRepresentantes(reps);
    } catch (err) {
        console.error(err);
        showAlert(`Falha ao conectar ao serviço de representantes via Gateway: ${err.message}`, "danger");
    }
}

function renderTabelaRepresentantes(reps) {
    const tbody = document.getElementById("tabelaRepresentantesBody");
    if (!reps || reps.length === 0) {
        tbody.innerHTML = `<tr><td colspan="3" class="text-center text-muted py-4">Nenhum representante cadastrado.</td></tr>`;
        return;
    }
    tbody.innerHTML = reps.map(r => `
        <tr>
            <td class="fw-bold">${r.id}</td>
            <td><span class="badge bg-warning-subtle text-dark border border-warning-subtle">${escapeHtml(r.cpf)}</span></td>
            <td class="fw-semibold">${escapeHtml(r.nome)}</td>
        </tr>
    `).join("");
}

async function handleCadastrarRepresentante(event) {
    event.preventDefault();
    const cpf = document.getElementById("repCpf").value.trim();
    const nome = document.getElementById("repNome").value.trim();

    try {
        const response = await fetch(`${API_BASE}/representantes`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ cpf, nome })
        });

        if (response.status === 201) {
            const novo = await response.json();
            showAlert(`Representante "${novo.nome}" cadastrado com sucesso!`, "success");
            document.getElementById("formRepresentante").reset();
            carregarRepresentantes();
        } else {
            const erro = await response.json().catch(() => ({}));
            showAlert(`Erro ao cadastrar representante: ${erro.message || response.statusText}`, "danger");
        }
    } catch (err) {
        showAlert(`Falha de comunicação com Gateway: ${err.message}`, "danger");
    }
}

async function buscarRepresentantePorNome() {
    const nome = document.getElementById("buscaRepNome").value.trim();
    try {
        const response = await fetch(`${API_BASE}/representantes/busca?nome=${encodeURIComponent(nome)}`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const reps = await response.json();
        renderTabelaRepresentantes(reps);
        showAlert(`Busca de representantes por "${nome}": ${reps.length} resultado(s).`, "info");
    } catch (err) {
        showAlert(`Erro ao buscar representantes: ${err.message}`, "danger");
    }
}

async function buscarRepresentantePorCpf() {
    const cpf = document.getElementById("buscaRepCpf").value.trim();
    if (!cpf) {
        carregarRepresentantes();
        return;
    }
    try {
        const response = await fetch(`${API_BASE}/representantes/cpf/${encodeURIComponent(cpf)}`);
        if (response.status === 404) {
            showAlert(`Nenhum representante encontrado com CPF ${cpf}.`, "warning");
            renderTabelaRepresentantes([]);
            return;
        }
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const rep = await response.json();
        renderTabelaRepresentantes([rep]);
        showAlert(`Representante encontrado com CPF ${cpf}!`, "success");
    } catch (err) {
        showAlert(`Erro ao consultar representante por CPF: ${err.message}`, "danger");
    }
}

// Popular dados iniciais de demonstração
async function popularDadosIniciais() {
    try {
        // Peças demo
        await fetch(`${API_BASE}/pecas`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ numeroIdentificacao: "ENG-001", nome: "Engrenagem Cônica", descricao: "Aço temperado 1045" })
        });
        await fetch(`${API_BASE}/pecas`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ numeroIdentificacao: "ROL-002", nome: "Rolamento Blindado", descricao: "Rolamento rígido de esferas 6205-2RS" })
        });

        // Clientes demo
        await fetch(`${API_BASE}/clientes`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ cpf: "123.456.789-00", nome: "Carlos Eduardo Silva" })
        });
        await fetch(`${API_BASE}/clientes`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ cpf: "987.654.321-99", nome: "Mariana Albuquerque" })
        });

        // Representantes demo
        await fetch(`${API_BASE}/representantes`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ cpf: "111.222.333-44", nome: "Felipe Jung" })
        });
        await fetch(`${API_BASE}/representantes`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ cpf: "555.666.777-88", nome: "Beatriz Nogueira" })
        });

        showAlert("Dados de demonstração populados com sucesso em todos os microsserviços!", "success");
        carregarPecas();
        carregarClientes();
        carregarRepresentantes();
    } catch (err) {
        showAlert(`Erro ao popular dados: ${err.message}`, "danger");
    }
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// Inicialização automática ao carregar página
document.addEventListener("DOMContentLoaded", () => {
    carregarPecas();
    carregarClientes();
    carregarRepresentantes();
});
