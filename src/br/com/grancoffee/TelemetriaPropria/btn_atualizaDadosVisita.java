package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_atualizaDadosVisita implements AcaoRotinaJava {
	int c = 0;
	int c2 = 0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		for(int i=0; i < linhas.length; i++) {
			start(linhas[i], arg0);
		}
		
		arg0.setMensagemRetorno("<br/><b>Aten��o</b><br/><br/> Visitas que tiveram contagem e foram ajustadas: "+c+"\nVisitas que n�o tiveram contagem: "+c2+"<br/><br/>");
		
	}

	private void start(Registro linhas, ContextoAcao arg0) {
		BigDecimal numos = (BigDecimal) linhas.getCampo("NUMOS");
		BigDecimal id = (BigDecimal) linhas.getCampo("ID");
		String patrimonio = (String) linhas.getCampo("CODBEM");
		
		if (verificaSeHouveContagem(numos)) {
			c++;
			//arg0.setMensagemRetorno("Houve contagem, "+c+" visitas atualizadas.");
			verificaTeclas(numos,id,patrimonio);
		} else {
			c2++;
			//arg0.setMensagemRetorno("N�o houve contagem!");
		}

	}

	private boolean verificaSeHouveContagem(BigDecimal numos) {
		boolean valida = false;

		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(QTDCONTAGEM) AS QTD FROM AD_APPCONTAGEM WHERE NUMOS=" + numos);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count >= 1) {
					valida = true;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		return valida;
	}
	
	private void verificaTeclas(BigDecimal numos, BigDecimal id, String patrimonio) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ITENSRETABAST",
					"this.ID=?", new Object[] { id }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				String tecla = VO.asString("TECLA");
				BigDecimal produto = VO.asBigDecimal("CODPROD");
				BigDecimal qtdContagem = null;
				String ajustado = VO.asString("AJUSTADO");
				
				DynamicVO contagem = getContagem(numos,tecla,produto);
				if(contagem!=null) {
					qtdContagem = contagem.asBigDecimal("QTDCONTAGEM");
				}else {
					qtdContagem = new BigDecimal(0);
				}
				
				if(!"S".equals(ajustado)) {
					VO.setProperty("CONTAGEM", qtdContagem);
				}

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[verificaTeclas] nao foi possivel verificar as teclas! patrimonio: "+patrimonio+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private DynamicVO getContagem(BigDecimal numos, String tecla, BigDecimal produto) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_APPCONTAGEM");
		DynamicVO VO = DAO.findOne("this.NUMOS=? AND this.CODPROD=? AND this.TECLA=?",new Object[] { numos, produto, tecla });
		return VO;
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "btn_atualizaDadosVisita");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui n�o tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
