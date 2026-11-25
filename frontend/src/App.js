import { useCallback, useEffect, useMemo, useState } from 'react';
import './App.css';

const API = process.env.REACT_APP_API_URL || '/api';
const INTERVALO_ATUALIZACAO = 15000;

const ROTULO_STATUS = {
  EM_ATENDIMENTO_BOT: 'Com o chatbot',
  AGUARDANDO_HUMANO: 'Aguardando humano',
  EM_ATENDIMENTO_HUMANO: 'Em atendimento humano',
  ENCERRADA: 'Encerrada',
};

const ROTULO_ORIGEM = {
  CLIENTE: 'Cliente',
  BOT: 'Chatbot',
  ATENDENTE: 'Atendente',
};

async function chamarApi(caminho, opcoes = {}) {
  const { headers, ...resto } = opcoes;
  let resposta;

  try {
    resposta = await fetch(`${API}${caminho}`, {
      ...resto,
      headers: { 'Content-Type': 'application/json', ...headers },
    });
  } catch (erro) {
    throw new Error(
      'Não foi possível conectar ao backend. Verifique se ele está em execução na porta 8080.'
    );
  }

  const texto = await resposta.text();
  let dados = null;
  if (texto) {
    try {
      dados = JSON.parse(texto);
    } catch (erro) {
      dados = null;
    }
  }

  if (!resposta.ok) {
    throw new Error(dados?.mensagem || `A requisição falhou (HTTP ${resposta.status}).`);
  }
  return dados;
}

function formatarEspera(segundos) {
  const total = Number(segundos);
  if (!Number.isFinite(total) || total < 60) return 'agora';

  const minutos = Math.floor(total / 60);
  if (minutos < 60) return `${minutos} min`;

  const horas = Math.floor(minutos / 60);
  const resto = minutos % 60;
  return resto ? `${horas}h ${String(resto).padStart(2, '0')}` : `${horas}h`;
}

function formatarHorario(valor) {
  if (!valor) return '—';
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) return '—';
  return data.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

function iniciais(nome = '') {
  return nome
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((parte) => parte[0])
    .join('')
    .toUpperCase() || '?';
}

function Avatar({ nome }) {
  const tom = [...nome].reduce((soma, letra) => soma + letra.charCodeAt(0), 0) % 4;
  return <span className={`avatar avatar--${tom}`}>{iniciais(nome)}</span>;
}

function Status({ status }) {
  return <span className={`status status--${(status || '').toLowerCase()}`}>{ROTULO_STATUS[status] || status}</span>;
}

function Vazio({ titulo, texto }) {
  return (
    <div className="vazio">
      <span className="vazio__icone" aria-hidden="true">✓</span>
      <h3>{titulo}</h3>
      <p>{texto}</p>
    </div>
  );
}

function ModalDetalhe({ conversa, onFechar }) {
  useEffect(() => {
    const aoPressionar = (evento) => {
      if (evento.key === 'Escape') onFechar();
    };
    document.addEventListener('keydown', aoPressionar);
    return () => document.removeEventListener('keydown', aoPressionar);
  }, [onFechar]);

  return (
    <div
      className="modal-fundo"
      onMouseDown={(evento) => evento.target === evento.currentTarget && onFechar()}
    >
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="titulo-detalhe">
        <div className="modal__cabecalho">
          <div className="pessoa">
            <Avatar nome={conversa.nomeCliente} />
            <div>
              <h2 id="titulo-detalhe">{conversa.nomeCliente}</h2>
              <p>{conversa.telefoneCliente}</p>
            </div>
          </div>
          <button type="button" className="botao-fechar" onClick={onFechar} aria-label="Fechar detalhes">
            ×
          </button>
        </div>

        <div className="modal__meta">
          <Status status={conversa.status} />
          <span className="tag">{conversa.equipe || 'Sem equipe'}</span>
          <span className="tag tag--suave">{conversa.assunto || 'Sem assunto'}</span>
        </div>

        <div className="modal__historico">
          {conversa.mensagens.length === 0 ? (
            <p className="modal__sem-mensagens">Ainda não há mensagens nesta conversa.</p>
          ) : (
            conversa.mensagens.map((mensagem) => (
              <div className={`mensagem mensagem--${mensagem.origem.toLowerCase()}`} key={mensagem.id}>
                <div className="mensagem__meta">
                  <strong>{ROTULO_ORIGEM[mensagem.origem] || mensagem.origem}</strong>
                  <span>{formatarHorario(mensagem.enviadaEm)}</span>
                </div>
                <p>{mensagem.conteudo}</p>
              </div>
            ))
          )}
        </div>

        <div className="modal__rodape">
          <span>{conversa.nomeAtendente ? `Atendente: ${conversa.nomeAtendente}` : 'Sem atendente humano'}</span>
          <span>Na fila desde {formatarHorario(conversa.entrouNaFilaEm)}</span>
        </div>
      </div>
    </div>
  );
}

function App() {
  const [fila, setFila] = useState([]);
  const [atendentes, setAtendentes] = useState([]);
  const [atendimentos, setAtendimentos] = useState([]);
  const [atendenteId, setAtendenteId] = useState('');
  const [equipeFiltro, setEquipeFiltro] = useState('Todas');
  const [carregandoInicial, setCarregandoInicial] = useState(true);
  const [sincronizando, setSincronizando] = useState(false);
  const [assumindoId, setAssumindoId] = useState(null);
  const [carregandoDetalhe, setCarregandoDetalhe] = useState(false);
  const [detalhe, setDetalhe] = useState(null);
  const [erro, setErro] = useState('');
  const [aviso, setAviso] = useState('');
  const [conexaoOk, setConexaoOk] = useState(true);
  const [ultimaAtualizacao, setUltimaAtualizacao] = useState(null);

  const atendenteSelecionado = atendentes.find((item) => String(item.id) === atendenteId) || null;

  useEffect(() => {
    let montado = true;

    async function iniciar() {
      try {
        const [dadosFila, dadosAtendentes] = await Promise.all([
          chamarApi('/fila'),
          chamarApi('/atendentes'),
        ]);
        if (!montado) return;

        setFila(dadosFila);
        setAtendentes(dadosAtendentes);
        const salvo = window.localStorage.getItem('wxn:atendente');
        const preferido =
          dadosAtendentes.find((item) => String(item.id) === salvo && item.disponivel) ||
          dadosAtendentes.find((item) => item.disponivel) ||
          dadosAtendentes[0];
        if (preferido) setAtendenteId(String(preferido.id));
        setUltimaAtualizacao(new Date());
        setConexaoOk(true);
      } catch (falha) {
        if (montado) {
          setErro(falha.message);
          setConexaoOk(false);
        }
      } finally {
        if (montado) setCarregandoInicial(false);
      }
    }

    iniciar();
    return () => {
      montado = false;
    };
  }, []);

  useEffect(() => {
    if (!atendenteId) {
      setAtendimentos([]);
      return undefined;
    }

    window.localStorage.setItem('wxn:atendente', atendenteId);
    let ativo = true;
    chamarApi(`/atendimentos?atendenteId=${encodeURIComponent(atendenteId)}`)
      .then((dados) => ativo && setAtendimentos(dados))
      .catch((falha) => ativo && setErro(falha.message));
    return () => {
      ativo = false;
    };
  }, [atendenteId]);

  const sincronizar = useCallback(
    async (silencioso = false) => {
      if (!silencioso) setSincronizando(true);
      try {
        const [dadosFila, dadosAtendentes] = await Promise.all([
          chamarApi('/fila'),
          chamarApi('/atendentes'),
        ]);
        setFila(dadosFila);
        setAtendentes(dadosAtendentes);
        if (atendenteId) {
          const dadosAtendimentos = await chamarApi(
            `/atendimentos?atendenteId=${encodeURIComponent(atendenteId)}`
          );
          setAtendimentos(dadosAtendimentos);
        }
        setUltimaAtualizacao(new Date());
        setConexaoOk(true);
        if (!silencioso) setErro('');
      } catch (falha) {
        setConexaoOk(false);
        if (!silencioso) setErro(falha.message);
      } finally {
        if (!silencioso) setSincronizando(false);
      }
    },
    [atendenteId]
  );

  useEffect(() => {
    if (carregandoInicial) return undefined;
    const intervalo = setInterval(() => sincronizar(true), INTERVALO_ATUALIZACAO);
    return () => clearInterval(intervalo);
  }, [carregandoInicial, sincronizar]);

  useEffect(() => {
    if (!aviso) return undefined;
    const timer = setTimeout(() => setAviso(''), 5000);
    return () => clearTimeout(timer);
  }, [aviso]);

  const equipes = useMemo(
    () => ['Todas', ...new Set(fila.map((item) => item.equipe).filter(Boolean))],
    [fila]
  );

  useEffect(() => {
    if (equipeFiltro !== 'Todas' && !equipes.includes(equipeFiltro)) {
      setEquipeFiltro('Todas');
    }
  }, [equipes, equipeFiltro]);

  const filaVisivel = useMemo(
    () => (equipeFiltro === 'Todas' ? fila : fila.filter((item) => item.equipe === equipeFiltro)),
    [fila, equipeFiltro]
  );

  const maiorEspera = fila.reduce((maior, item) => Math.max(maior, item.esperaEmSegundos || 0), 0);

  async function assumirConversa(item) {
    if (!atendenteSelecionado) {
      setErro('Selecione um atendente antes de assumir uma conversa.');
      return;
    }
    if (!atendenteSelecionado.disponivel) {
      setErro('O atendente selecionado está indisponível.');
      return;
    }

    setAssumindoId(item.conversaId);
    setErro('');
    try {
      await chamarApi(`/fila/${item.conversaId}/assumir`, {
        method: 'POST',
        body: JSON.stringify({ atendenteId: atendenteSelecionado.id }),
      });
      setAviso(`Conversa de ${item.nomeCliente} assumida por ${atendenteSelecionado.nome}.`);
      await sincronizar(true);
    } catch (falha) {
      setErro(falha.message);
      await sincronizar(true);
    } finally {
      setAssumindoId(null);
    }
  }

  async function abrirDetalhe(conversaId) {
    setCarregandoDetalhe(true);
    try {
      setDetalhe(await chamarApi(`/conversas/${conversaId}`));
    } catch (falha) {
      setErro(falha.message);
    } finally {
      setCarregandoDetalhe(false);
    }
  }

  if (carregandoInicial) {
    return (
      <div className="tela-carregando">
        <span className="marca__simbolo">W</span>
        <span className="spinner spinner--grande" aria-hidden="true" />
        <p>Conectando ao painel de atendimento...</p>
      </div>
    );
  }

  return (
    <div className="painel">
      <header className="topo">
        <div className="marca">
          <span className="marca__simbolo">W</span>
          <div>
            <strong className="marca__nome">WXN</strong>
            <span className="marca__subtitulo">Atendimento humano</span>
          </div>
        </div>
        <div className="topo__acoes">
          <span className={`conexao ${conexaoOk ? '' : 'conexao--off'}`}>
            <span className="conexao__ponto" />
            {conexaoOk ? 'Conectado' : 'Sem conexão'}
          </span>
          <span className="topo__atualizacao">
            {ultimaAtualizacao ? `Atualizado às ${formatarHorario(ultimaAtualizacao)}` : 'Aguardando atualização'}
          </span>
          <button
            type="button"
            className="botao-icone"
            onClick={() => sincronizar()}
            disabled={sincronizando}
            title="Atualizar agora"
            aria-label="Atualizar agora"
          >
            <span className={sincronizando ? 'girando' : ''}>↻</span>
          </button>
        </div>
      </header>

      <main className="conteudo">
        <section className="abertura">
          <div>
            <span className="etiqueta">Segunda história · Atendimento humano</span>
            <h1>Assuma conversas e continue o atendimento</h1>
            <p>
              O chatbot encaminha para a fila os casos que precisam de uma pessoa. Escolha seu
              nome, acompanhe a espera e assuma a próxima conversa.
            </p>
          </div>

          <div className="cartao-atendente">
            <label htmlFor="seletor-atendente">Atendente logado</label>
            <select
              id="seletor-atendente"
              value={atendenteId}
              onChange={(evento) => setAtendenteId(evento.target.value)}
            >
              {!atendenteSelecionado && <option value="">Selecione um atendente</option>}
              {atendentes.map((atendente) => (
                <option
                  key={atendente.id}
                  value={atendente.id}
                  disabled={!atendente.disponivel && String(atendente.id) !== atendenteId}
                >
                  {atendente.nome} — {atendente.equipe}
                  {atendente.disponivel ? '' : ' (indisponível)'}
                </option>
              ))}
            </select>
            <div className="cartao-atendente__rodape">
              <span className={`ponto ${atendenteSelecionado?.disponivel ? 'ponto--on' : 'ponto--off'}`} />
              {atendenteSelecionado
                ? atendenteSelecionado.disponivel
                  ? `Disponível · ${atendenteSelecionado.equipe}`
                  : 'Indisponível para novas conversas'
                : 'Nenhum atendente cadastrado'}
            </div>
          </div>
        </section>

        <section className="indicadores" aria-label="Resumo do atendimento">
          <article className="indicador">
            <span className="indicador__rotulo">Na fila</span>
            <strong className="indicador__valor">{fila.length}</strong>
            <span className="indicador__detalhe">aguardando humano</span>
          </article>
          <article className="indicador">
            <span className="indicador__rotulo">Comigo</span>
            <strong className="indicador__valor">{atendimentos.length}</strong>
            <span className="indicador__detalhe">em atendimento</span>
          </article>
          <article className="indicador">
            <span className="indicador__rotulo">Disponíveis</span>
            <strong className="indicador__valor">
              {atendentes.filter((atendente) => atendente.disponivel).length}
            </strong>
            <span className="indicador__detalhe">atendentes online</span>
          </article>
          <article className="indicador indicador--destaque">
            <span className="indicador__rotulo">Maior espera</span>
            <strong className="indicador__valor">{formatarEspera(maiorEspera)}</strong>
            <span className="indicador__detalhe">na fila agora</span>
          </article>
        </section>

        {erro && (
          <div className="alerta alerta--erro" role="alert">
            <span className="alerta__icone">!</span>
            <p>{erro}</p>
            <button type="button" onClick={() => setErro('')} aria-label="Fechar aviso">×</button>
          </div>
        )}

        {aviso && (
          <div className="alerta alerta--sucesso" role="status">
            <span className="alerta__icone">✓</span>
            <p>{aviso}</p>
          </div>
        )}

        <div className="colunas">
          <section className="secao">
            <div className="secao__cabecalho">
              <div>
                <h2>
                  Fila de espera <span className="contador">{filaVisivel.length}</span>
                </h2>
                <p>As conversas mais antigas aparecem primeiro.</p>
              </div>
              <div className="filtros" role="group" aria-label="Filtrar por equipe">
                {equipes.map((equipe) => (
                  <button
                    type="button"
                    key={equipe}
                    className={`filtro ${equipeFiltro === equipe ? 'filtro--ativo' : ''}`}
                    onClick={() => setEquipeFiltro(equipe)}
                  >
                    {equipe}
                  </button>
                ))}
              </div>
            </div>

            {filaVisivel.length === 0 ? (
              <Vazio
                titulo="Fila zerada"
                texto="Quando o chatbot encaminhar uma conversa para humano, ela aparece aqui."
              />
            ) : (
              <div className="lista">
                {filaVisivel.map((item) => (
                  <article className="cartao-conversa" key={item.conversaId}>
                    <div className="cartao-conversa__topo">
                      <div className="pessoa">
                        <Avatar nome={item.nomeCliente} />
                        <div>
                          <h3>{item.nomeCliente}</h3>
                          <p>{item.telefoneCliente}</p>
                        </div>
                      </div>
                      <span
                        className={`espera ${
                          item.esperaEmSegundos >= 600
                            ? 'espera--critica'
                            : item.esperaEmSegundos >= 300
                              ? 'espera--atencao'
                              : ''
                        }`}
                      >
                        <span className="espera__rotulo">Espera</span>
                        {formatarEspera(item.esperaEmSegundos)}
                      </span>
                    </div>

                    <p className="cartao-conversa__assunto">
                      {item.assunto || 'Sem assunto informado'}
                    </p>

                    <div className="tags">
                      <span className="tag">{item.equipe || 'Sem equipe'}</span>
                      <span className="tag tag--suave">Aguardando humano</span>
                    </div>

                    <div className="cartao-conversa__acoes">
                      <button
                        type="button"
                        className="botao botao--texto"
                        onClick={() => abrirDetalhe(item.conversaId)}
                      >
                        Ver histórico
                      </button>
                      <button
                        type="button"
                        className="botao botao--primario"
                        onClick={() => assumirConversa(item)}
                        disabled={assumindoId !== null || !atendenteSelecionado?.disponivel}
                      >
                        {assumindoId === item.conversaId ? (
                          <>
                            <span className="spinner" aria-hidden="true" /> Assumindo...
                          </>
                        ) : (
                          'Assumir conversa'
                        )}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>

          <section className="secao secao--lateral">
            <div className="secao__cabecalho">
              <div>
                <h2>Meus atendimentos</h2>
                <p>
                  {atendenteSelecionado
                    ? `Conversas assumidas por ${atendenteSelecionado.nome}.`
                    : 'Selecione um atendente para ver a lista.'}
                </p>
              </div>
            </div>

            {atendimentos.length === 0 ? (
              <Vazio
                titulo="Nenhuma conversa assumida"
                texto={
                  atendenteSelecionado
                    ? 'Assuma uma conversa da fila para começar o atendimento.'
                    : 'Escolha quem está atendendo no seletor acima.'
                }
              />
            ) : (
              <div className="lista">
                {atendimentos.map((item) => (
                  <article className="cartao-atendimento" key={item.conversaId}>
                    <div className="cartao-atendimento__topo">
                      <div className="pessoa">
                        <Avatar nome={item.nomeCliente} />
                        <div>
                          <h3>{item.nomeCliente}</h3>
                          <p>{item.telefoneCliente}</p>
                        </div>
                      </div>
                      <Status status={item.status} />
                    </div>
                    <p className="cartao-atendimento__assunto">
                      {item.assunto || 'Sem assunto informado'}
                    </p>
                    <div className="tags">
                      <span className="tag">{item.equipe || 'Sem equipe'}</span>
                      <span className="tag tag--suave">Na fila desde {formatarHorario(item.entrouNaFilaEm)}</span>
                    </div>
                    <button
                      type="button"
                      className="botao botao--secundario"
                      onClick={() => abrirDetalhe(item.conversaId)}
                    >
                      Abrir conversa
                    </button>
                  </article>
                ))}
              </div>
            )}
          </section>
        </div>
      </main>

      <footer className="rodape">
        <span>WXN · Painel de atendimento</span>
        <span>Atualização automática a cada 15 segundos</span>
      </footer>

      {detalhe && <ModalDetalhe conversa={detalhe} onFechar={() => setDetalhe(null)} />}

      {carregandoDetalhe && (
        <div className="carregando-detalhe" role="status">
          <span className="spinner" aria-hidden="true" /> Carregando histórico...
        </div>
      )}
    </div>
  );
}

export default App;
