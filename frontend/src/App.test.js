import { render, screen } from '@testing-library/react';
import App from './App';

function resposta(dados) {
  return Promise.resolve({
    ok: true,
    status: 200,
    text: () => Promise.resolve(JSON.stringify(dados)),
  });
}

beforeEach(() => {
  global.fetch = jest.fn((url) => {
    if (url.endsWith('/fila')) {
      return resposta([
        {
          conversaId: 1,
          nomeCliente: 'Maria Silva',
          telefoneCliente: '5581999990000',
          assunto: 'Segunda via de boleto',
          equipe: 'Financeiro',
          entrouNaFilaEm: '2026-11-25T10:00:00',
          esperaEmSegundos: 720,
        },
      ]);
    }
    if (url.endsWith('/atendentes')) {
      return resposta([
        {
          id: 1,
          nome: 'Ana Lima',
          email: 'ana@wxn.com',
          equipe: 'Suporte',
          disponivel: true,
        },
      ]);
    }
    return resposta([]);
  });
});

afterEach(() => {
  jest.restoreAllMocks();
});

test('mostra a fila e o seletor de atendente', async () => {
  render(<App />);

  expect(
    await screen.findByRole('heading', { name: /assuma conversas e continue o atendimento/i })
  ).toBeInTheDocument();
  expect(await screen.findByText('Maria Silva')).toBeInTheDocument();
  expect(screen.getByText('Segunda via de boleto')).toBeInTheDocument();
  expect(screen.getByLabelText('Atendente logado')).toHaveValue('1');
});
