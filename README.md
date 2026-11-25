# Projeto-3

# 🤖 Análise e Implementação de Chatbots — Projeto WXN

![Status](https://img.shields.io/badge/Status-Em%20Desenvolvimento-blue)
![Foco](https://img.shields.io/badge/Foco-Atendimento%20Automatizado-success)

## 📌 Sobre o Projeto

Este repositório é dedicado ao **estudo, análise comparativa e desenvolvimento de uma solução de atendimento automatizado via chatbot** para a empresa **WXN**.

O objetivo principal é desenvolver uma solução de atendimento inteligente integrada ao WhatsApp, utilizando Inteligência Artificial para auxiliar na compreensão das solicitações dos clientes e na geração de respostas. A proposta também contempla um painel de controle para acompanhamento dos atendimentos e dos indicadores operacionais, permitindo o suporte da equipe interna e o encaminhamento para atendimento humano quando necessário.

A arquitetura definida utiliza **Java com Spring Boot** no backend, **React** no frontend, **PostgreSQL** como banco de dados e prevê integração com a API do modelo **Grok**, da xAI, e com a **WhatsApp Business Platform**, da Meta.

---

## 🎯 Objetivos

- 🔍 **Mapeamento de Necessidades:** Analisar o fluxo de atendimento da WXN para identificar gargalos e oportunidades de automação.
- ⚡ **Inteligência Artificial:** Avaliar e integrar recursos de IA para interpretar as solicitações dos clientes e auxiliar na geração de respostas.
- 🛠️ **Desenvolvimento da Solução:** Construir uma aplicação com backend em Java e Spring Boot e frontend em React, seguindo a arquitetura definida para o projeto.
- 💬 **Automação do Atendimento:** Planejar a integração com o WhatsApp Business para facilitar o atendimento automatizado e o encaminhamento para atendentes humanos.
- 📊 **Análise de Métricas:** Definir e acompanhar indicadores de desempenho, como tempo de resposta, taxa de resolução e volume de atendimentos.
- 🔐 **Segurança e Confiabilidade:** Proteger credenciais, dados e integrações, considerando os requisitos de segurança da aplicação.

---

## 🛠️ Tecnologias e Frameworks

| Ferramenta / Tecnologia | Finalidade | Status |
| :--- | :--- | :--- |
| **Java 17** | Linguagem de programação do backend | 🟢 Estrutura criada |
| **Spring Boot** | Framework para desenvolvimento do backend e APIs | 🟢 Estrutura criada |
| **React** | Desenvolvimento da interface web e do painel de controle | 🟢 Estrutura criada |
| **JavaScript, HTML e CSS** | Desenvolvimento da interface frontend | 🟢 Em utilização |
| **PostgreSQL** | Persistência dos dados da aplicação | 🟡 Configuração local preparada |
| **Maven** | Gerenciamento de dependências e execução do backend | 🟢 Configurado |
| **Grok API (xAI)** | Interpretação de solicitações e geração de respostas com IA | ⚪ Planejado |
| **WhatsApp Business Platform** | Canal de comunicação com os clientes | ⚪ Planejado |
| **Git e GitHub** | Controle de versão e colaboração no desenvolvimento | 🟢 Em utilização |
| **Visual Studio Code** | Ambiente de desenvolvimento | 🟢 Em utilização |
| **Vercel** | Hospedagem planejada do frontend | ⚪ Planejado |
| **Render** | Hospedagem planejada do backend | ⚪ Planejado |

---

## 🚀 Como Instalar e Executar Localmente

### 📋 Pré-requisitos

Antes de iniciar, instale as seguintes ferramentas:

- **Java 17** ou superior compatível com o projeto.
- **Node.js e npm** para executar o frontend React.
- **PostgreSQL** para o banco de dados.
- **Git** para clonar o repositório.
- **Maven** (opcional, pois o projeto inclui o Maven Wrapper).

### 📥 1. Clonar o repositório

Abra o terminal e execute:

```bash
git clone <URL_DO_REPOSITORIO>
cd Projeto-3
```

Substitua `<URL_DO_REPOSITORIO>` pela URL do repositório no GitHub.

### 🗄️ 2. Configurar o banco de dados

No PostgreSQL, crie o banco de dados e o usuário utilizados pela aplicação:

```sql
CREATE USER projeto_user WITH PASSWORD 'sua_senha';
CREATE DATABASE projeto_db OWNER projeto_user;
```

Caso o usuário ou o banco já existam, não é necessário criá-los novamente. Ajuste os dados conforme a configuração do seu ambiente.

Configure as variáveis de ambiente utilizadas pelo backend:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/projeto_db"
export DB_USERNAME="projeto_user"
export DB_PASSWORD="sua_senha"
```

Substitua `sua_senha` pela senha definida para o usuário do PostgreSQL. Não compartilhe nem versione credenciais reais.

### ⚙️ 3. Executar o backend

Em um terminal, entre na pasta do backend:

```bash
cd backend
chmod +x mvnw
./mvnw spring-boot:run
```

O backend será iniciado na porta padrão `8080`, caso ela não tenha sido alterada na configuração.

**Tabelas do banco (migração automática):** não é preciso rodar nenhum SQL manualmente. Ao iniciar, o backend usa o **Flyway** para criar ou atualizar as tabelas no banco `projeto_db`, a partir dos scripts em `backend/src/main/resources/db/migration`. Na primeira execução, com o banco vazio, o log mostra `Successfully applied 1 migration`. Em seguida, o Hibernate confere se as tabelas correspondem às classes de entidade e interrompe a inicialização se houver divergência.

- Para alterar o esquema, crie um novo script (`V2__descricao.sql`, `V3__...`); nunca edite um script que já foi aplicado.
- Se o banco já tiver tabelas criadas manualmente, sem o histórico do Flyway, apague-as (ou recrie o banco) antes da primeira execução.

**Testes:** os testes usam um banco **H2 em memória** com a mesma migração, então não dependem do PostgreSQL:

```bash
./mvnw test
```

### 💻 4. Executar o frontend

Abra outro terminal, na pasta raiz do projeto, e execute:

```bash
cd frontend
npm install
npm start
```

O frontend React será iniciado, normalmente, em:

http://localhost:3000

### ✅ 5. Acessar a aplicação

Com o backend e o frontend em execução, acesse:

- **Frontend:** http://localhost:3000
- **Backend:** http://localhost:8080

Mantenha os dois terminais abertos enquanto estiver utilizando a aplicação.

### 💬 Painel de atendimento humano (segunda história)

Ao abrir o frontend, a tela **Assuma conversas e continue o atendimento** permite:

- selecionar o atendente logado;
- acompanhar a fila de conversas encaminhadas pelo chatbot, com assunto, equipe e tempo de espera;
- filtrar a fila por equipe;
- ver o histórico de mensagens antes de assumir;
- assumir uma conversa e acompanhá-la em **Meus atendimentos**;
- receber avisos quando a conversa já foi assumida por outra pessoa.

A tela atualiza a fila automaticamente a cada 15 segundos e consome a API do backend:

| Método | Endpoint | Uso |
| :--- | :--- | :--- |
| `GET` | `/api/fila` | Lista as conversas aguardando atendente humano |
| `GET` | `/api/atendentes` | Lista os atendentes para o seletor |
| `GET` | `/api/atendimentos?atendenteId={id}` | Lista as conversas em atendimento humano do atendente |
| `GET` | `/api/conversas/{id}` | Traz o detalhe e o histórico de mensagens |
| `POST` | `/api/fila/{id}/assumir` | Assume a conversa; corpo `{ "atendenteId": 1 }` |

Para facilitar a avaliação, o backend cria atendentes e conversas de exemplo quando o banco está vazio. Esse comportamento pode ser desativado com `WXN_DADOS_DEMO=false` antes de iniciar a aplicação.

**Observações:**
- O acesso ao banco de dados depende de o serviço PostgreSQL estar em execução e das credenciais estarem corretas.
- Caso o projeto utilize endpoints de API no frontend, a URL do backend deverá estar configurada de acordo com a implementação.
- A integração com a Grok API e com o WhatsApp Business Platform deverá ser configurada quando esses recursos forem implementados.
