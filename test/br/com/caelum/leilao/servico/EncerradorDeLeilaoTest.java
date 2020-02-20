package br.com.caelum.leilao.servico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;
import org.mockito.InOrder;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.infra.dao.LeilaoDao;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;
import br.com.caelum.leilao.infra.email.Carteiro;

public class EncerradorDeLeilaoTest {

	@Test
	public void deveEncerrarLeiloesQueComecaramUmaSemanaAtras() {

		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

		List<Leilao> leiloesAntigos = Arrays.asList(leilao1, leilao2);

		// criamos o mock
		RepositorioDeLeiloes daoFalso = mock(LeilaoDao.class);
		// ensinamos ele a retornar a lista de leiloes antigos
		when(daoFalso.correntes()).thenReturn(leiloesAntigos);

		Carteiro carteiroFalso = mock(Carteiro.class);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrador.encerra();

		// vamos conferir tambem o tamanho da lista!
		assertEquals(2, encerrador.getTotalEncerrados());
		assertTrue(leilao1.isEncerrado());
		assertTrue(leilao2.isEncerrado());
	}

	@Test
	public void naoDeveEncerrarLeiloesQueComecaramUmaSemanaAtras() {
		Calendar ontem = Calendar.getInstance();
		ontem.add(Calendar.DAY_OF_MONTH, -1);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(ontem).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(ontem).constroi();

		RepositorioDeLeiloes daoFalso = mock(LeilaoDao.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

		Carteiro carteiroFalso = mock(Carteiro.class);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrador.encerra();

		assertEquals(0, encerrador.getTotalEncerrados());
		assertFalse(leilao1.isEncerrado());
		assertFalse(leilao2.isEncerrado());
	}

	@Test
	public void naoDeveEncerrarLeiloesCasoNaoHajaNenhum() {
		RepositorioDeLeiloes daoFalso = mock(LeilaoDao.class);
		when(daoFalso.correntes()).thenReturn(new ArrayList<Leilao>());

		Carteiro carteiroFalso = mock(Carteiro.class);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrador.encerra();

		assertEquals(0, encerrador.getTotalEncerrados());
	}

	@Test
	public void deveAtualizarLeiloesEncerrados() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 02, 10);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();

		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1));

		Carteiro carteiroFalso = mock(Carteiro.class);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrador.encerra();

		// passamos os mocks que serao verificados
		InOrder inOrder = inOrder(daoFalso, carteiroFalso);
		// a primeira invocação
		inOrder.verify(daoFalso, times(1)).atualiza(leilao1);
		// a segunda invocação
		inOrder.verify(carteiroFalso, times(1)).envia(leilao1);
	}

	@Test
	public void naoDeveEncerrarLeiloesQueComecaramMenosDeUmaSemanaAtras() {

		Calendar ontem = Calendar.getInstance();
		ontem.add(Calendar.DAY_OF_MONTH, -1);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(ontem).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(ontem).constroi();

		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

		Carteiro carteiroFalso = mock(Carteiro.class);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrador.encerra();

		assertEquals(0, encerrador.getTotalEncerrados());
		assertFalse(leilao1.isEncerrado());
		assertFalse(leilao2.isEncerrado());

		verify(daoFalso, times(0)).atualiza(leilao1);
		verify(daoFalso, times(0)).atualiza(leilao2);
	}
	
	@Test
	public void deveContinuarAExecucaoMesmoQuandoODaoFalha() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 2, 21);
		
		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();
		
		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		Carteiro carteiroFalso = mock(Carteiro.class);
		
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		doThrow(new RuntimeException()).when(daoFalso).atualiza(leilao1);
		
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrador.encerra();
		
		verify(daoFalso).atualiza(leilao2);
		verify(carteiroFalso).envia(leilao2);
		
		verify(carteiroFalso, times(0)).envia(leilao1);
	}
	
	@Test
    public void deveDesistirSeDaoFalhaPraSempre() {
        Calendar antiga = Calendar.getInstance();
        antiga.set(1999, 1, 20);

        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma")
            .naData(antiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira")
            .naData(antiga).constroi();

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

        Carteiro carteiroFalso = mock(Carteiro.class);
        doThrow(new RuntimeException()).when(daoFalso).atualiza(any(Leilao.class));

        EncerradorDeLeilao encerrador = 
            new EncerradorDeLeilao(daoFalso, carteiroFalso);

        encerrador.encerra();

        verify(carteiroFalso, never()).envia(any(Leilao.class));
    }
}
