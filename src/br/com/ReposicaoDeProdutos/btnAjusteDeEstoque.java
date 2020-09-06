package br.com.ReposicaoDeProdutos;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btnAjusteDeEstoque implements AcaoRotinaJava {


	/**
	 * <h1> Bot�o Contagem APP </h1>
	 * 
	 * Objeto que pega as informa��es da tela App Invent�rio e insere na tela reposi��o de produtos.
	 * 
	 * @author gabriel.nascimento
	 */
	public void doAction(ContextoAcao arg0) throws Exception {

		BigDecimal contagem = new BigDecimal(0);

		/**
		 * Pega o c�digo da reposi��o que ser� usado para valida��es
		 */
		Registro[] linha = arg0.getLinhas();
		BigDecimal codigo = (BigDecimal) linha[0].getCampo("CODIGO");

		/**
		 * Pega o c�digo da contagem do APP (Digitdo pelo usu�rio)
		 * 
		 */
		Integer codContagem = (Integer) arg0.getParam("CONTAGEMAPP");

		/**
		 * Verifica na tabela contagem de estoque os dados correspondentes ao c�digo digitado.
		 */
		JapeWrapper contagemDAO = null;
		contagemDAO = JapeFactory.dao("ContagemEstoqueProduto");
		DynamicVO contagemAPP = null;
		contagemAPP = contagemDAO.findOne(" CODCONTAGEM=?", new Object[] { codContagem });

		/**
		 *  - Primeiro IF ele valida se o c�digo da contagem existe na tabela.
		 *  - Segundo IF ele valida se o c�digo do local de estoque � igual na reposi��o e na contagem.
		 *  
		 *  Se as valida��es passagem ele come�a com o procedimento
		 *  
		 *  1� - Procura da tabela de reposi��o os itens que correspondem a reposi��o atual.
		 *  2� - Guarda esses itens em uma cole��o e itera os elementos atrav�s do FOR percorrendo cada um deles.
		 *  3� - A cada item ele procura se esse mesmo item existe na tebela da contagem se existe ele pega o valor digitado no app e joga no valor da tela de reposi��o.
		 *  
		 */
		if (contagemAPP != null) {

			if (validaLocal(codigo, codContagem)) {

				try {

					EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
					Collection<?> itensREPO = dwfEntityFacade.findByDynamicFinder(
							new FinderWrapper("TGFREPOEST", "this.CODIGO = ? ", new Object[] { codigo }));
					Collection<?> itensAPP = dwfEntityFacade.findByDynamicFinder(new FinderWrapper(
							"ContagemEstoqueProduto", "this.CODCONTAGEM = ? ", new Object[] { codContagem }));
					
					/**
					 * Esse m�todo zera todas as quantidades dos produtos antes de substituir pelos valores corretos.
					 */
					zeraContagem(itensREPO, contagem);
					
					/**
					 * Inicio das itera��es das cole��es de itens das telas.
					 */
					for (Iterator<?> iteratorREPO = itensREPO.iterator(); iteratorREPO.hasNext();) { //primeiro for (tela reposi��o)

						PersistentLocalEntity itemEntity = (PersistentLocalEntity) iteratorREPO.next();
						DynamicVO itemVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
								.wrapInterface(DynamicVO.class);

						for (Iterator<?> iteratorAPP = itensAPP.iterator(); iteratorAPP.hasNext();) { //segundo for (tela contagem APP)
							
							PersistentLocalEntity itemAPPEntity = (PersistentLocalEntity) iteratorAPP.next();
							DynamicVO itemAppVO = (DynamicVO) ((DynamicVO) itemAPPEntity.getValueObject())
									.wrapInterface(DynamicVO.class);

							if (itemVO.getProperty("CODPROD").toString()
									.equals(itemAppVO.getProperty("CODPROD").toString())) { //If para saber se os produtos s�o iguais

								itemVO.setProperty("CONTAGEM", itemAppVO.getProperty("QTD")); //substitui��o dos itens tela app para reposi��o
								itemEntity.setValueObject((EntityVO) itemVO); //commit, sem isso n�o funciona !!
								
								gravaInformacoes(codContagem, linha); //grava na tela reposi��o dizendo que foram informa��es do app
								
								arg0.setMensagemRetorno("Dados carregados com sucesso!");
							}
						}

					}

				} catch (Exception e) {
					arg0.mostraErro("Erro:" + e.getMessage());
				}

			} else {
				arg0.mostraErro("Local de estoque da reposi��o n�o corresponde ao local de estoque da contagem!");
			}

		} else {
			arg0.mostraErro("C�digo de contagem inv�lida!");
		}

	}

	/**
	 * M�todo que zera todos os valores dos itens da tela de reposi��o.
	 * @param colecao
	 * @param valor
	 * @throws PersistenceException
	 */
	public void zeraContagem(Collection<?> colecao, BigDecimal valor) throws PersistenceException {

		for (Iterator<?> iteratorREPO = colecao.iterator(); iteratorREPO.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) iteratorREPO.next();
			DynamicVO itemVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			itemVO.setProperty("CONTAGEM", valor);
			itemEntity.setValueObject((EntityVO) itemVO);
		}
	}

	/**
	 * M�todo que valida se os locais de estoque da tela de reposi��o e contagem de app s�o iguais.
	 * @param codigo
	 * @param codContagem
	 * @return
	 * @throws Exception
	 */
	public boolean validaLocal(BigDecimal codigo, Integer codContagem) throws Exception {

		DynamicVO tabelaREPO = tabelaReposicao(codigo);
		DynamicVO tabelaAPP = tabelaContagem(codContagem);

		if (tabelaREPO.getProperty("CODLOCAL").equals(tabelaAPP.getProperty("CODLOCAL"))) {
			return true;
		} else {
			return false;
		}

	}
	
	/**
	 * M�todo que grava as informa��es do abastecedor, a data que foi feito a contagem e se ela foi feita por app.
	 * @param codigo
	 * @param codContagem
	 * @throws Exception
	 */
	public void gravaInformacoes(Integer codContagem, Registro[] linha) throws Exception {
		
		DynamicVO tabelaAPP = tabelaContagem(codContagem);
		
		linha[0].setCampo("AD_ABASTECEDOR", tabelaAPP.getProperty("CODUSU"));
		linha[0].setCampo("AD_FEITOPELOAPP", "S");
		linha[0].setCampo("AD_DATAINVENTARIO", tabelaAPP.getProperty("DHCONTAGEM").toString());
		linha[0].setCampo("CODCONTAGEMAPP", codContagem);
				
	}
	
	/**
	 * M�todo de apoio para identificar os dados da tabela de reposi��o
	 * @param codigo
	 * @return
	 * @throws Exception
	 */
	public DynamicVO tabelaReposicao(BigDecimal codigo) throws Exception {
		JapeWrapper tabelaDAO = null;
		tabelaDAO = JapeFactory.dao("TGFREPO");
		DynamicVO tabela = null;
		tabela = tabelaDAO.findOne(" CODIGO=?", new Object[] { codigo });
		return tabela;
	}
	
	/**
	 * M�todo de apoio para identificar os dados da tabela de contagem do APP
	 * @param codContagem
	 * @return
	 * @throws Exception
	 */
	public DynamicVO tabelaContagem(Integer codContagem) throws Exception {
		JapeWrapper tabela2DAO = null;
		tabela2DAO = JapeFactory.dao("ContagemEstoqueAvancado");
		DynamicVO tabela2 = null;
		tabela2 = tabela2DAO.findOne(" CODCONTAGEM=?", new Object[] { codContagem });
		return tabela2;
	}
	

}

