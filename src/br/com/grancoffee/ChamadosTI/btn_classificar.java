package br.com.grancoffee.ChamadosTI;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;

import com.sankhya.util.StringUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_classificar implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		if(linhas.length==1) {
			start(linhas,arg0);
		}			
	}
	
	public void start(Registro[] linhas,ContextoAcao arg0) throws Exception {			
		
		String email = (String) linhas[0].getCampo("EMAIL");
		BigDecimal numos = (BigDecimal) linhas[0].getCampo("NUMOS");
		String tipo = validaTipo((String) arg0.getParam("TIPOSOLICITACAO"));
		String prioridade = validaPrioridade((String) arg0.getParam("PRIORIDADE"));
		String nivel = validaNivel((String) arg0.getParam("NIVEL"));
		String classificacao = validaClassificacao((String) arg0.getParam("CLASSIFICACAO"));
		Timestamp dataFinal = (Timestamp) linhas[0].getCampo("DTFECHAMENTO");
		String descricaoAbreviada = StringUtils.substr(linhas[0].getCampo("DESCRICAO").toString(), 0, 100);
		
		if(dataFinal!=null) {
			arg0.mostraErro("Chamado encerrado, n�o pode ser classificado!");
		}else {
			enviarEmail(numos,email,tipo,classificacao,prioridade,nivel,descricaoAbreviada);
			setDados(linhas,arg0);
		}
	}
	
	public void setDados(Registro[] linhas,ContextoAcao arg0) throws Exception {
		linhas[0].setCampo("TIPO", arg0.getParam("TIPOSOLICITACAO"));
		linhas[0].setCampo("CLASSIFICACAO", arg0.getParam("CLASSIFICACAO"));
		linhas[0].setCampo("PRIORIDADE", arg0.getParam("PRIORIDADE"));
		linhas[0].setCampo("NIVEL", arg0.getParam("NIVEL"));
		linhas[0].setCampo("ATENDENTE", null);
		linhas[0].setCampo("DTPREVISTA", null);
	}
	
	private void enviarEmail(BigDecimal numos, String email, String tipo, String classificacao, String prioridade, String nivel, String descricao) throws Exception {
		
		try {
			String mensagem = new String();
			
			mensagem = "Prezado,<br/><br/> "
					+ "O seu chamado de n�mero <b>"+numos+"</b> foi classificado."
					+ "<br/><br/><i>\""+descricao+" ...\"</i>"
					+ "<br/><br/><b>Classifica��o:</b> "+classificacao
					+ "<br/><br/><b>Tipo:</b> "+tipo
					+ "<br/><br/><b>Prioridade:</b> "+prioridade
					+ "<br/><br/><b>Nivel Atendimento:</b> "+nivel
					+ "<br/><br/><b>Esta � uma mensagem autom�tica, por gentileza n�o respond�-la</b>"
					+ "<br/><br/>Atencionamente,"
					+ "<br/>Departamento TI"
					+ "<br/>Gran Coffee Com�rcio, Loca��o e Servi�os S.A."
					+ "<br/>"
					+ "<img src=http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-pq.png  alt=\"\"/>";
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODFILA", getUltimoCodigoFila());
			VO.setProperty("DTENTRADA", new Timestamp(System.currentTimeMillis()));
			VO.setProperty("MENSAGEM", mensagem.toCharArray());
			VO.setProperty("TIPOENVIO", "E");
			VO.setProperty("ASSUNTO", new String("CHAMADO - "+numos));
			VO.setProperty("EMAIL", email);
			VO.setProperty("CODUSU", new BigDecimal(0));
			VO.setProperty("STATUS", "Pendente");
			VO.setProperty("CODCON", new BigDecimal(0));		
			
			dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("## [ChamadosTI.evento_criaOS] ## - NAO FOI POSSIVEL ENVIAR E-MAIL DE CONFIRMACAO DE ABERTURA DE CHAMADO"+e.getMessage());
			e.printStackTrace();
		}	
	}
	
	public String validaTipo(String tipo) {
		String tipoClassificado="";
		
		switch (tipo) {
		case "1":tipoClassificado="Analise";
		break;
		
		case "2":tipoClassificado="Incidente";
		break;

		default:
			tipoClassificado="Analise";
			break;
		}
		
		return tipoClassificado;
	}
	
	public String validaPrioridade(String tipo) {
		String tipoClassificado="";
		
		switch (tipo) {
		case "1":tipoClassificado="Baixa";
		break;
		
		case "2":tipoClassificado="M�dia";
		break;
		
		case "3":tipoClassificado="Alta";
		break;
		
		case "4":tipoClassificado="Urgente";
		break;

		default:
			tipoClassificado="Baixa";
			break;
		}
		
		return tipoClassificado;
	}
	 
	public String validaNivel(String tipo) {
		String tipoClassificado="";
		
		switch (tipo) {
		case "1":tipoClassificado="Analise Nivel 1";
		break;
		
		case "2":tipoClassificado="Analise Nivel 2";
		break;
		
		case "3":tipoClassificado="Analise Nivel 3";
		break;
		
		case "4":tipoClassificado="Desenvolvimento";
		break;
		
		case "5":tipoClassificado="Gerencia";
		break;

		default:
			tipoClassificado="Analise Nivel 1";
			break;
		}
		
		return tipoClassificado;
	}
	
	public String validaClassificacao(String tipo) throws Exception {
		String descricao="";
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT DESCRICAO FROM AD_TIPOCHAMADOSTI WHERE CODTIPO ="+tipo);
		contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			descricao = contagem.getString("DESCRICAO");
		}
		
		return descricao;
	}
	
	private BigDecimal getUltimoCodigoFila() throws Exception {
		int count = 0;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(CODFILA)+1 AS CODFILA FROM TMDFMG");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getInt("CODFILA");
		}
		
		BigDecimal ultimoCodigo = new BigDecimal(count);
		
		return ultimoCodigo;
	}
	
}
