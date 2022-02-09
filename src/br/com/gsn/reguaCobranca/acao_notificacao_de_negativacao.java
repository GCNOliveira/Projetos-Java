package br.com.gsn.reguaCobranca;

import java.math.BigDecimal;
import java.util.Collection;
import com.sankhya.util.TimeUtils;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.extensions.reguacobranca.AcaoReguaCobranca;
import br.com.sankhya.extensions.reguacobranca.ContextoRegua;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class acao_notificacao_de_negativacao implements AcaoReguaCobranca  {

	@Override
	public void execute(ContextoRegua arg0) throws Exception {
		
		Collection<Registro> titulos = arg0.getTitulos();
		BigDecimal codigo = new BigDecimal(20);
		
		for(Registro a : titulos) {
			BigDecimal nufin = (BigDecimal) a.getCampo("NUFIN");
			
			DynamicVO TGFFIN = getTGFFIN(nufin);
			
			if(TGFFIN!=null) {
				BigDecimal statusAnterior = TGFFIN.asBigDecimal("AD_STATUSREGUA");
				
				atualizaTGFFIN(nufin, codigo);
				salvaHistorico(statusAnterior,nufin,codigo);
			}
			
		}
		 
	}
	
	private DynamicVO getTGFFIN(BigDecimal nufin) throws Exception {
		DynamicVO tgffin = null;
		
		JapeWrapper DAO = JapeFactory.dao("Financeiro");
		DynamicVO VO = DAO.findOne("NUFIN=?",new Object[] { nufin });
		if(VO!=null) {
			tgffin = VO;
		}
		return tgffin;
	}
	
	private void atualizaTGFFIN(BigDecimal nufin, BigDecimal codigo) throws Exception {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			PersistentLocalEntity PersistentLocalEntity = dwfFacade.findEntityByPrimaryKey("Financeiro", nufin);
			EntityVO NVO = PersistentLocalEntity.getValueObject();
			DynamicVO appVO = (DynamicVO) NVO;
							 
			appVO.setProperty("AD_STATUSREGUA", codigo);
							 
			PersistentLocalEntity.setValueObject(NVO);
		} catch (Exception e) {
			salvarException("[atualizaTGFFIN] N�o foi poss�vel alterar o financeiro! "+"\n"+"N�mero Fin: "+nufin+"\n"+e.getMessage()+"\n"+e.getCause());
		}

	}

	private void salvaHistorico(BigDecimal statusAnterior,BigDecimal nufin, BigDecimal codigo) throws Exception {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_HISTREGUA");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("NUFIN", nufin);
			VO.setProperty("DATA", TimeUtils.getNow());
			VO.setProperty("STATUSANT", statusAnterior);
			VO.setProperty("STATUSATUAL", codigo);
			VO.setProperty("CODUSU", new BigDecimal(0));
			
			dwfFacade.createEntity("AD_HISTREGUA", (EntityVO) VO);
		} catch (Exception e) {
			salvarException("[salvaHistorico] N�o foi poss�vel salvar no hist�rico! "+"\n"+"N�mero Fin: "+nufin+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "acao_notificacao_de_negativacao");
			VO.setProperty("PACOTE", "br.com.gsn.reguaCobranca");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", new BigDecimal(0));
			VO.setProperty("ERRO", mensagem);
			
			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
			
		} catch (Exception e) {
			//aqui n�o tem jeito rs tem que mostrar no log
			System.out.println("## [salvarException] ## - Nao foi possivel salvar a Exception! "+e.getMessage());
		}
	}

}