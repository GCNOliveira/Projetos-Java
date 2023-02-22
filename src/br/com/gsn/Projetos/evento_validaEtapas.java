package br.com.gsn.Projetos;

import java.sql.Timestamp;
import com.sankhya.util.TimeUtils;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class evento_validaEtapas implements EventoProgramavelJava {

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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
	//	validacoes(arg0);
		insert(arg0);
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		validacoes(arg0);
		update(arg0);
	}
	
	private void insert(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String status = VO.asString("STATUS");
		Timestamp dtfim = VO.asTimestamp("DTFIM");
		Timestamp dtprevini = VO.asTimestamp("DTPREVINI");
		
		if("2".equals(status)) {
			VO.setProperty("DTFIM", TimeUtils.getNow());
		}
		
		if(dtfim!=null) {
			VO.setProperty("STATUS", "2");
		}
		
		if(dtprevini!=null) {
			VO.setProperty("STATUS", "1");
		}
		
	/*	if(status==null) {
			VO.setProperty("STATUS", "1");
		}
		*/
	}
	
	private void update(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		String status = VO.asString("STATUS");
		String oldstatus = oldVO.asString("STATUS");
		
		Timestamp dtfim = VO.asTimestamp("DTFIM");
		Timestamp olddtfim = oldVO.asTimestamp("DTFIM");
		Timestamp dtprevini = VO.asTimestamp("DTPREVINI");
		Timestamp olddtprevini = oldVO.asTimestamp("DTPREVINI");
		
		if(status!=oldstatus) {
			if("2".equals(status)) {
				VO.setProperty("DTFIM", TimeUtils.getNow());
			}
			
			if("1".equals(status)) {
				VO.setProperty("DTFIM", null);
			}
		}
		
		if(dtfim!=olddtfim) {
			if(dtfim!=null) {
				VO.setProperty("STATUS", "2");
			}
		}
	
		if(dtprevini!=olddtprevini) {
			if(dtprevini!=null) {
				VO.setProperty("STATUS", "1");
			}
			
			if(dtprevini==null) {
				VO.setProperty("STATUS", null);
			}
		}
	}
	

	
	private void validacoes(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		String mensagem = "";
		String status = VO.asString("STATUS");
		String oldstatus = oldVO.asString("STATUS");
		Timestamp dtprevini = VO.asTimestamp("DTPREVINI");
		Timestamp olddtprevini = oldVO.asTimestamp("DTPREVINI");
		Timestamp dtprevfim = VO.asTimestamp("DTPREVFIM");
		Timestamp olddtprevfim = oldVO.asTimestamp("DTPREVFIM");
		
		if(status!=oldstatus) {
			if(status != null) {
					if(dtprevini==null) {
						mensagem = "<b>A 'Dt. Prev. Inicio' precisa ser informada!</b>";
						throw new Error(mensagem);
					}
					if(dtprevfim==null) {
						mensagem = "<b>A 'Dt. Prev. Fim' precisa ser informada!</b>";
						throw new Error(mensagem);
					}
			}
			
		}
	}
}
