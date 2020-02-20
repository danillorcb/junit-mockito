package br.com.caelum.leilao.servico;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Calendar;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.dominio.Pagamento;
import br.com.caelum.leilao.dominio.Usuario;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;
import br.com.caelum.leilao.infra.dao.RepositorioDePagamentos;
import br.com.caelum.leilao.infra.relogio.Relogio;

public class GeradorDePagamentoTest {

	@Test
	public void deveGerarPagamentoParaUmLeilaoEncerrado() {

		RepositorioDeLeiloes leiloes = mock(RepositorioDeLeiloes.class);
		RepositorioDePagamentos pagamentos = mock(RepositorioDePagamentos.class);
//		Avaliador avaliador = mock(Avaliador.class);

		Leilao leilao = new CriadorDeLeilao().para("Playstation")
				.lance(new Usuario("José da Silva"), 2000.0)
				.lance(new Usuario("Maria Pereira"), 2500.0)
				.constroi();

		when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));
//		when(avaliador.getMaiorLance()).thenReturn(2500.0);

		GeradorDePagamento gerador = new GeradorDePagamento(leiloes, pagamentos, new Avaliador());
		gerador.gera();

		// criamos o ArgumentCaptor que sabe capturar um Pagamento
	    ArgumentCaptor<Pagamento> argumento = ArgumentCaptor.forClass(Pagamento.class);
	    
	    // capturamos o Pagamento que foi passado para o método salvar
	    verify(pagamentos).salva(argumento.capture());
	    
		Pagamento pagamentoGerado = argumento.getValue();
		assertEquals(2500.0, pagamentoGerado.getValor(), 0.00001);	
		
	}
	
	@Test
	public void deveEmpurrarPagamentoNoSabadoParaOProximoDiaUtil() {
		RepositorioDeLeiloes leiloes = mock(RepositorioDeLeiloes.class);
		RepositorioDePagamentos pagamentos = mock(RepositorioDePagamentos.class);
		Relogio relogio = mock(Relogio.class);
		
		// dia 15/02/2020 é um sábado
		Calendar sabado = Calendar.getInstance();
		sabado.set(2020, Calendar.FEBRUARY, 15);

		// ensinamos o mock a dizer que "hoje" é sabado!
		when(relogio.hoje()).thenReturn(sabado);
		
		Leilao leilao = new CriadorDeLeilao().para("Playstation")
				.lance(new Usuario("José da Silva"), 2000.0)
				.lance(new Usuario("Maria Pereira"), 2500.0)
				.constroi();
		
		when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));
		
		GeradorDePagamento gerador = new GeradorDePagamento(leiloes, pagamentos, new Avaliador(), relogio);
		gerador.gera();
		
		ArgumentCaptor<Pagamento> argument = ArgumentCaptor.forClass(Pagamento.class);
		verify(pagamentos).salva(argument.capture());
		Pagamento pagamentoGerado = argument.getValue();
		
		assertEquals(Calendar.MONDAY, pagamentoGerado.getData().get(Calendar.DAY_OF_WEEK));
		assertEquals(17, pagamentoGerado.getData().get(Calendar.DAY_OF_MONTH));
	}
	
	@Test
	public void deveEmpurrarPagamentoNoDomingoParaOProximoDiaUtil() {
		RepositorioDeLeiloes leiloes = mock(RepositorioDeLeiloes.class);
		RepositorioDePagamentos pagamentos = mock(RepositorioDePagamentos.class);
		Relogio relogio = mock(Relogio.class);
		
		Calendar domingo = Calendar.getInstance();
		domingo.set(2020, Calendar.FEBRUARY, 16);
		when(relogio.hoje()).thenReturn(domingo);
		
		Leilao leilao = new CriadorDeLeilao().para("Playstation")
				.lance(new Usuario("José da Silva"), 2000.0)
				.lance(new Usuario("Maria Pereira"), 2500.0)
				.constroi();
		
		when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));
		
		GeradorDePagamento gerador = new GeradorDePagamento(leiloes, pagamentos, new Avaliador(), relogio);
		gerador.gera();
		
		ArgumentCaptor<Pagamento> argument = ArgumentCaptor.forClass(Pagamento.class);
		verify(pagamentos).salva(argument.capture());
		Pagamento pagamentoGerado = argument.getValue();
		
		assertEquals(Calendar.MONDAY, pagamentoGerado.getData().get(Calendar.DAY_OF_WEEK));
		assertEquals(17, pagamentoGerado.getData().get(Calendar.DAY_OF_MONTH));
	}
}