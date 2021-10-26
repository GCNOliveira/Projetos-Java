package br.com.grancoffee.Ecommerce;

import java.math.BigDecimal;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_valida_exclusao implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		start(arg0);
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	private void start(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal nunota = VO.asBigDecimal("NUNOTA");
		String idVtex = VO.asString("AD_PEDIDOVTEX");

		if (idVtex != null) {
			registraCancelamento(nunota, idVtex);
		}

	}

	private void registraCancelamento(BigDecimal nunota, String id) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_VTEXCANCELADOS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("NUNOTA", nunota);
			VO.setProperty("IDVTEX", id);
			VO.setProperty("DATA", TimeUtils.getNow());
			VO.setProperty("CODUSU", getUsuario());

			dwfFacade.createEntity("AD_VTEXCANCELADOS", (EntityVO) VO);

		} catch (

		Exception e) {
			// TODO: handle exception
		}
	}
	
	private BigDecimal getUsuario() {
		BigDecimal usu = null;
		try {
			usu = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		if(usu==null) {
			usu = new BigDecimal(0);
		}
		
		return usu;
	}

}
