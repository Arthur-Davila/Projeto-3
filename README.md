# Projeto-3

# 🤖 Análise e Implementação de Chatbots — Projeto WXN

![Status](https://img.shields.io/badge/Status-Em%20Desenvolvimento-blue)
![Foco](https://img.shields.io/badge/Foco-Atendimento%20Automatizado-success)

## 📌 Sobre o Projeto

Este repositório é dedicado ao **estudo, análise comparativa e desenvolvimento de uma solução de atendimento automatizado via chatbot** para a empresa **WXN**.

O objetivo principal é desenvolver uma solução de atendimento inteligente integrada ao WhatsApp, utilizando Inteligência Artificial para auxiliar na compreensão das solicitações dos clientes e na geração de respostas. A proposta também contempla um painel de controle para acompanhamento dos atendimentos e dos indicadores operacionais, permitindo o suporte da equipe interna e o encaminhamento para atendimento humano quando necessário.

A arquitetura definida utiliza **Java com Spring Boot** no backend, **React** no frontend, **PostgreSQL** como banco de dados e prevê integração com a API da **Groq Cloud** e com a **WhatsApp Business Platform**, da Meta.

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
| **Spring Boot** | Backend REST, persistência e webhooks | 🟢 Em utilização |
| **React** | Desenvolvimento da interface web e do painel de controle | 🟢 Estrutura criada |
| **JavaScript, HTML e CSS** | Desenvolvimento da interface frontend | 🟢 Em utilização |
| **PostgreSQL** | Persistência dos dados da aplicação | 🟡 Configuração local preparada |
| **Maven** | Gerenciamento de dependências e execução do backend | 🟢 Configurado |
| **Groq Cloud** | Geração de respostas do chatbot pela Chat Completions API | 🟡 Código integrado; chave Groq pendente |
| **WhatsApp Business Platform** | Webhook de entrada e envio de mensagens de texto | 🟡 Código integrado; credenciais e configuração Meta pendentes |
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

Para desenvolvimento local, também é possível salvar `DB_PASSWORD=sua_senha` em `backend/.env`; o Spring carrega esse arquivo automaticamente ao executar o backend a partir da pasta `backend`. O arquivo `.env` é ignorado pelo Git.

### ⚙️ 3. Executar o backend

Em um terminal, entre na pasta do backend:

```bash
cd backend
chmod +x mvnw
./mvnw spring-boot:run
```

No Windows, use o wrapper `.cmd`:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

O backend será iniciado na porta padrão `8080`, caso ela não tenha sido alterada na configuração.

**Tabelas do banco (migração automática):** não é preciso rodar nenhum SQL manualmente. Ao iniciar, o backend usa o **Flyway** para criar ou atualizar as tabelas no banco `projeto_db`, a partir dos scripts em `backend/src/main/resources/db/migration`. Em um banco vazio, o Flyway aplica as duas migrações versionadas atuais (`V1` e `V2`). Em seguida, o Hibernate confere se as tabelas correspondem às classes de entidade e interrompe a inicialização se houver divergência.

- Para alterar o esquema, crie um novo script (`V3__descricao.sql`, `V4__...`); nunca edite um script que já foi aplicado.
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

No Windows, se o PowerShell bloquear os scripts `npm.ps1` com o erro
`PSSecurityException`, use os executáveis `.cmd`:

```powershell
cd frontend
npm.cmd install
npm.cmd start
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
| `POST` | `/api/conversas/{id}/mensagens` | Registra e, com WhatsApp configurado, envia uma resposta do atendente |

Para facilitar a avaliação, o backend cria atendentes e conversas de exemplo quando o banco está vazio. Esse comportamento pode ser desativado com `WXN_DADOS_DEMO=false` antes de iniciar a aplicação.

### 🤖 6. Integrar o chatbot com Groq Cloud e WhatsApp

O backend recebe mensagens de texto da WhatsApp Cloud API em `/api/webhooks/whatsapp` e salva o histórico no PostgreSQL. Enquanto a conversa estiver com o bot, ele usa respostas iniciais ou a Chat Completions API da Groq Cloud; quando estiver na fila ou com um atendente, a mensagem apenas entra no histórico para o atendimento continuar. Mensagens de outros tipos, como áudio e imagem, ainda são ignoradas. O painel mostra se as credenciais da Groq Cloud e do WhatsApp estão preenchidas em `GET /api/integracoes`.

1. Crie uma chave da Groq Cloud e preencha `GROQ_API_KEY` no arquivo `backend/.env`. O modelo padrão é `llama-3.3-70b-versatile`; pode ser alterado com `GROQ_MODEL`.
2. No Meta for Developers, configure um app com WhatsApp Cloud API. Copie o token de acesso, o ID do número de telefone e o segredo do app para `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID` e `WHATSAPP_APP_SECRET`.
3. Crie um valor aleatório para `WHATSAPP_VERIFY_TOKEN` e use o mesmo valor no painel de webhooks da Meta e em `backend/.env`.
4. Publique o backend em uma URL HTTPS acessível pela Meta e cadastre `https://SEU-DOMINIO/api/webhooks/whatsapp` como callback. Inscreva o campo `messages`.
5. Reinicie o backend. O painel deve indicar Groq e WhatsApp como configurados.

Use `backend/.env.example` como referência para as variáveis. Não substitua seu `backend/.env` atual sem copiar também as credenciais do banco; mantenha chaves reais fora do Git.

O bot já tem respostas iniciais para saudações, boletos, faturas, acesso ao aplicativo e cancelamento. Boleto e fatura seguem para Financeiro; cancelamento vai para Retenção; pedidos explícitos de atendente seguem para Suporte. Para perguntas gerais, a Groq responde com as últimas 16 mensagens e uma instrução para não inventar preços, prazos ou regras da WXN. Fatos adicionais aprovados podem ser informados em `WXN_FAQ`, em uma única linha.

Sem chave da Groq, as respostas iniciais continuam disponíveis. Sem credenciais WhatsApp, as mensagens podem ser vistas no histórico local, mas não são entregues ao cliente. A empresa precisa obter consentimento (*opt-in*) do cliente antes de enviar mensagens pelo WhatsApp. Mensagens livres de atendimento podem ser enviadas dentro da janela de 24 horas iniciada quando o cliente envia uma mensagem ou faz uma chamada; depois dela, só podem ser enviados templates aprovados pela Meta. Consulte as regras oficiais de [opt-in](https://developers.facebook.com/documentation/business-messaging/whatsapp/getting-opt-in) e da [janela de atendimento](https://developers.facebook.com/documentation/business-messaging/whatsapp/messages/send-messages#customer-service-windows).

| Método | Endpoint | Uso |
| :--- | :--- | :--- |
| `GET` | `/api/integracoes` | Informa se Groq e WhatsApp têm credenciais configuradas |
| `GET` | `/api/webhooks/whatsapp` | Valida o callback da Meta com o verify token |
| `POST` | `/api/webhooks/whatsapp` | Recebe mensagens e valida a assinatura SHA-256 da Meta |
| `POST` | `/api/conversas/{id}/mensagens` | Registra e envia a resposta do atendente pelo WhatsApp quando configurado |

O endpoint `GET /api/integracoes` verifica se as variáveis necessárias estão preenchidas; ele não valida se as chaves são aceitas pela Meta ou pela Groq. Para receber webhooks, a URL precisa ser pública e usar HTTPS. Em produção com frontend e backend em domínios diferentes, configure também a URL do backend no frontend (`REACT_APP_API_URL`) e permita a origem do frontend no CORS do backend com `WXN_CORS_ALLOWED_ORIGINS`.

### Publicação em Vercel, Render e Neon

O código aceita frontend e backend em domínios separados. O backend libera por padrão `http://localhost:3000`; em produção, configure `WXN_CORS_ALLOWED_ORIGINS` com a origem exata do frontend, sem barra no final. Para mais de uma origem, separe os endereços por vírgula. Não use `*` em produção.

1. Crie um banco PostgreSQL na Neon e configure `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no serviço do backend. Use a URL JDBC do endpoint Neon e mantenha o parâmetro SSL exigido pelo provedor.
2. Publique `backend` no Render usando o Maven Wrapper. Configure as variáveis do banco, `WXN_DADOS_DEMO=false`, `WXN_CORS_ALLOWED_ORIGINS=https://SEU-FRONTEND.vercel.app` e as credenciais de integração que já estão listadas em `backend/.env.example`.
3. Publique `frontend` na Vercel e configure `REACT_APP_API_URL=https://SEU-BACKEND.onrender.com/api` antes do build. A URL precisa terminar em `/api`.
4. Depois do deploy, confira a tela do painel, o endpoint `/api/integracoes` e os logs do backend. Configure o webhook da Meta somente após confirmar que a URL pública do backend está acessível em HTTPS.

Essas instruções preparam o projeto para publicação; não indicam que os serviços já foram criados ou que as chaves externas foram validadas. O painel atual mostra indicadores operacionais da fila em tempo real (volume aguardando, atendimentos do agente, agentes disponíveis e maior espera). Métricas históricas dependem de requisitos e regras de cálculo definidos para a entrega.
