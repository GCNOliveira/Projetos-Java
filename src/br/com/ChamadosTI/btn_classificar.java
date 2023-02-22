package br.com.ChamadosTI;

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
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

public class btn_classificar implements AcaoRotinaJava {
  public void doAction(ContextoAcao arg0) throws Exception {
    Registro[] linhas = arg0.getLinhas();
    if (linhas.length == 1)
      start(linhas, arg0); 
  }
  
  public void start(Registro[] linhas, ContextoAcao arg0) throws Exception {
    Timestamp dataFinal = (Timestamp)linhas[0].getCampo("DTFECHAMENTO");
    String status = "1";
    BigDecimal numos = (BigDecimal)linhas[0].getCampo("NUMOS");
    if (dataFinal != null) {
      arg0.mostraErro("Chamado encerrado, nao pode ser classificado!");
    } else {
      setDados(linhas, arg0);
      alteraStatusOs(status, numos, arg0, linhas);
    } 
  }
  
  public void setDados(Registro[] linhas, ContextoAcao arg0) throws Exception {
    linhas[0].setCampo("TIPO", arg0.getParam("TIPOSOLICITACAO"));
    linhas[0].setCampo("CLASSIFICACAO", arg0.getParam("CLASSIFICACAO"));
    linhas[0].setCampo("PRIORIDADE", arg0.getParam("PRIORIDADE"));
    linhas[0].setCampo("NIVEL", arg0.getParam("NIVEL"));
    linhas[0].setCampo("ATENDENTE", null);
    linhas[0].setCampo("DTPREVISTA", null);
    linhas[0].setCampo("AREA", arg0.getParam("AREA"));
    linhas[0].setCampo("RECORRENTE", arg0.getParam("RECORRENTE"));
  }
  
  public String validaTipo(String tipo) {
    String tipoClassificado = "";
    String str1;
    switch ((str1 = tipo).hashCode()) {
      case 49:
        if (!str1.equals("1"))
          break; 
        tipoClassificado = "Analise";
        return tipoClassificado;
      case 50:
        if (!str1.equals("2"))
          break; 
        tipoClassificado = "Incidente";
        return tipoClassificado;
    } 
    tipoClassificado = "Analise";
    return tipoClassificado;
  }
  
  public String validaArea(String area) {
    String tipoClassificado = "";
    String str1;
    switch ((str1 = area).hashCode()) {
      case 49:
        if (!str1.equals("1"))
          break; 
        tipoClassificado = "Infraestrutura";
        return tipoClassificado;
      case 50:
        if (!str1.equals("2"))
          break; 
        tipoClassificado = "Sistemas";
        return tipoClassificado;
    } 
    tipoClassificado = "T.I";
    return tipoClassificado;
  }
  
  public String validaPrioridade(String tipo) {
    String tipoClassificado = "";
    String str1;
    switch ((str1 = tipo).hashCode()) {
      case 49:
        if (!str1.equals("1"))
          break; 
        tipoClassificado = "Baixa";
        return tipoClassificado;
      case 50:
        if (!str1.equals("2"))
          break; 
        tipoClassificado = "M";
        return tipoClassificado;
      case 51:
        if (!str1.equals("3"))
          break; 
        tipoClassificado = "Alta";
        return tipoClassificado;
      case 52:
        if (!str1.equals("4"))
          break; 
        tipoClassificado = "Urgente";
        return tipoClassificado;
    } 
    tipoClassificado = "Baixa";
    return tipoClassificado;
  }
  
  public String validaNivel(String tipo) {
    String tipoClassificado = "";
    String str1;
    switch ((str1 = tipo).hashCode()) {
      case 49:
        if (!str1.equals("1"))
          break; 
        tipoClassificado = "Analise Nivel 1";
        return tipoClassificado;
      case 50:
        if (!str1.equals("2"))
          break; 
        tipoClassificado = "Analise Nivel 2";
        return tipoClassificado;
      case 51:
        if (!str1.equals("3"))
          break; 
        tipoClassificado = "Analise Nivel 3";
        return tipoClassificado;
      case 52:
        if (!str1.equals("4"))
          break; 
        tipoClassificado = "Desenvolvimento";
        return tipoClassificado;
      case 53:
        if (!str1.equals("5"))
          break; 
        tipoClassificado = "Gerencia";
        return tipoClassificado;
    } 
    tipoClassificado = "Analise Nivel 1";
    return tipoClassificado;
  }
  
  public String validaClassificacao(String tipo) throws Exception {
    String descricao = "";
    JdbcWrapper jdbcWrapper = null;
    EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
    jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
    NativeSql nativeSql = new NativeSql(jdbcWrapper);
    nativeSql.resetSqlBuf();
    nativeSql.appendSql("SELECT DESCRICAO FROM AD_TIPOCHAMADOSTI WHERE CODTIPO =" + tipo);
    ResultSet contagem = nativeSql.executeQuery();
    while (contagem.next())
      descricao = contagem.getString("DESCRICAO"); 
    return descricao;
  }
  
  private void alteraStatusOs(String status, BigDecimal numos, ContextoAcao arg0, Registro[] linhas) throws Exception {
	    try {
	      EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
	      Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("OrdemServico", 
	            "this.NUMOS=?", new Object[] { numos }));
	      for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext(); ) {
	        PersistentLocalEntity itemEntity = (PersistentLocalEntity)Iterator.next();
	        EntityVO NVO = (EntityVO)((DynamicVO)itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
	        DynamicVO VO = (DynamicVO)NVO;
	        VO.setProperty("CODCOS", new BigDecimal(status));
	        itemEntity.setValueObject(NVO);
	      } 
	    } catch (Exception e) {
	      salvarException("[alteraStatusOs] - NAO FOI POSSIVEL ALTERAR O STATUS DA OS! " + e.getMessage() + "\n" + e.getMessage());
	    } 
	  }

  
  private void salvarException(String mensagem) {
	    try {
	      EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
	      EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
	      DynamicVO VO = (DynamicVO)NPVO;
	      VO.setProperty("OBJETO", "btn_statusOS");
	      VO.setProperty("PACOTE", "br.com.grancoffee.ChamadosTI");
	      VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
	      VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
	      VO.setProperty("ERRO", mensagem);
	      dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO)VO);
	    } catch (Exception e) {
	      System.out.println("## [btn_classificar] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
	    } 
	  }
  
  
  
}
