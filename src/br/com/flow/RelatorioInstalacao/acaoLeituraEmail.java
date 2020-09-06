package br.com.flow.RelatorioInstalacao;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import org.jsoup.Jsoup;
import com.sun.mail.imap.protocol.FLAGS;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.SWRepositoryUtils;

public class acaoLeituraEmail implements ScheduledAction {
	
	public JapeSession.SessionHandle hnd; 
	private String resp = null;
	private String http = "192.168.0.80:8280";
	private String codusu = "2201";
	private String nomeusu = "FLOW";
	private String senha = "123456";
	private String jsessionID = null;
	private String programa = "7"; //para editar na mudan�a do flow
	private String version = null;
	private static String PATH_ANEXO = "/Sistema/workflow/formularios/";
	private String nomeInstancia = "arquivos";
	private String chaveMD5 = buildChaveArquivo();
	private String nomeArquivo = null;
	
	private String conteudo = null;
	private String emailSolicitante = null;
	private String assuntoEmail = null;
	private String dtEmail = null;
	private BigDecimal idflow = null;
	
	private String campoQueRecebeAnexoComercial = "EMAIL_ANEXO"; //para editar na mudan�a do flow
	private String campoIdDoEmail = "EMAIL_ID"; //para editar na mudan�a do flow

	
	public void onTime(ScheduledActionContext arg0) {
		
		hnd =null;
		
		try {
			
			hnd = JapeSession.open();
			hnd.execWithTX(new JapeSession.TXBlock() {

				public void doWithTx() throws Exception {
					
					start(); 
				}
				
			});
			
		} catch (Exception e) {
			System.out.println("Nao foi possivel ler os e-mails! "+e.getMessage());
		}
		
	}
	
	private void start() {
		
		  String pop3Host = "outlook.office365.com";
		  String mailStoreType = "pop3";	
		  final String userName = "flow@grancoffee.com.br";
		  final String password = "Info@2015";

		  receiveEmail(pop3Host, mailStoreType, userName, password);
	}
	
	public void receiveEmail(String pop3Host,String mailStoreType, String userName, String password){
		
		//Set properties
	    Properties props = new Properties();
	    props.put("mail.store.protocol", "pop3");
	    props.put("mail.pop3.host", pop3Host);
	    props.put("mail.pop3.port", "995");
	    props.put("mail.pop3.starttls.enable", "true");
	    
	    // Get the Session object.
	    Session session = Session.getInstance(props);
	   //session.setDebug(true);
	    
	    try {
	    	//Create the POP3 store object and connect to the pop store.
	    	Store store = session.getStore("pop3s");
	    	store.connect(pop3Host, userName, password);
	    	
	    	//Create the folder object and open it in your mailbox.
	    	Folder emailFolder = store.getFolder("INBOX");
	    	emailFolder.open(Folder.READ_WRITE);
	    	
	    	//Retrieve the messages from the folder object.
	    	Message[] messages = emailFolder.getMessages();
	    	System.out.println("Total Message: " + messages.length);
	    	
	    	//List<File> attachments = new ArrayList<File>();
	    	
	    	//Iterate the messages
	    	for (int i = 0; i < messages.length; i++) {
	    		
	    		 Message message = messages[i];
	    		 BigDecimal id = verificaUltimoID();

	    		 if(message!=null) {
	    			 verificaEmail(message, id);
	    			 criaTarefaFlow(id);
	    			 insertAnexoNoFluxoFLow(id);
	    			 enviarEmail(emailSolicitante,idflow);
	    		 }
	    		 
	    		 message.setFlag(FLAGS.Flag.DELETED, true);
	    	}
	    	//close the folder and store objects
	 	   emailFolder.close(true);
	 	   store.close();
	    } catch (MessagingException e){
			e.printStackTrace();
		} catch (Exception e) {
		       e.printStackTrace();
		}
	}

	public void salvaDados(String assunto, Address remetente, BigDecimal id, Date data) throws Exception {

		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EMAILFLOW");
		DynamicVO VO = (DynamicVO) NPVO;
		
		emailSolicitante = remetente.toString();
		assuntoEmail = assunto;
		dtEmail = com.sankhya.util.StringUtils.formatTimestamp(new Timestamp(data.getTime()), "dd/MM/YYYY");
		
		VO.setProperty("ID", id);
		VO.setProperty("ASSUNTO", assunto);
		VO.setProperty("REMETENTE", remetente.toString());
		VO.setProperty("DTEMAIL", new Timestamp(data.getTime()));

		dwfFacade.createEntity("AD_EMAILFLOW", (EntityVO) VO);
		
		
	}
	
	private void criaTarefaFlow(BigDecimal id) throws Exception {
		
		String url = "http://"+http+"/mge/service.sbr?serviceName=MobileLoginSP.login";
		String request1 = "<serviceRequest serviceName=\"MobileLoginSP.login\">\r\n" +
				  " <requestBody>\r\n" + " <NOMUSU>"+nomeusu+"</NOMUSU>\r\n" +
				  " <INTERNO>"+senha+"</INTERNO>\r\n" + " </requestBody>\r\n" +
				  " </serviceRequest>";
		
		Post_JSON(url,request1);
		jsessionID = getJssesionId(resp);
		version = pegaVersao().toString();
		
		String emailSolicitanteOriginal = emailSolicitante;
		String aux = emailSolicitanteOriginal.substring(emailSolicitanteOriginal.indexOf("<")+1,emailSolicitanteOriginal.lastIndexOf(">"));
		
		String query_url = "http://"+http+"/workflow/service.sbr?serviceName=ListaTarefaSP.startProcess&counter=79&application=ListaTarefa&mgeSession="+jsessionID;
		//String request = "{\"serviceName\":\"ListaTarefaSP.startProcess\",\"requestBody\":{\"param\":{\"codPrn\":5,\"formulario\":{\"nativo\":[],\"embarcado\":[{\"entityName\":\"PROCESS_5_VERSION_+"+version+"\",\"parentEntity\":\"-99999999\",\"records\":[{\"record\":[{\"name\":\"EMAIL\",\"value\":\""+id.toString()+"\"}]}],\"configFields\":[],\"detalhes\":[]}],\"formatado\":[]}},\"clientEventList\":{\"clientEvent\":[{\"$\":\"br.com.sankhya.workflow.listatarefa.necessita.variavel.inicializacao\"}]}}}";
		String request2 = "{\"serviceName\":\"ListaTarefaSP.startProcess\",\"requestBody\":{\"param\":{\"codPrn\":"+programa+",\"formulario\":{\"nativo\":[],\"embarcado\":[{\"entityName\":\"PROCESS_"+programa+"_VERSION_+"+version+"\",\"parentEntity\":\"-99999999\",\"records\":[{\"record\":[{\"name\":\"EMAIL_ID\",\"value\":\""+id.toString()+"\"},{\"name\":\"EMAIL_SOLICITANTE\",\"value\":\""+aux+"\"},{\"name\":\"EMAIL_ASSUNTO\",\"value\":\""+assuntoEmail+"\"},{\"name\":\"EMAIL_DATA\",\"value\":\""+dtEmail+"\"},{\"name\":\"EMAIL_CONTEUDO\",\"value\":\""+conteudo+"\"}]}],\"configFields\":[],\"detalhes\":[]}],\"formatado\":[]}},\"clientEventList\":{\"clientEvent\":[{\"$\":\"br.com.sankhya.workflow.listatarefa.necessita.variavel.inicializacao\"}]}}}";
		
		Post_JSON(query_url,request2);
		//System.out.println("ID JESSIONNNN: "+jsessionID+"\n VERSAOOO: "+version);
	}
		
	private void salvaAnexo(BigDecimal id, BigDecimal nroAnexo, byte[] anexo, String nome) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_ANEXOSEMAILFLOW");
		DynamicVO VO = (DynamicVO) NPVO;

		VO.setProperty("ID", id);
		VO.setProperty("NRANEXO", nroAnexo);
		VO.setProperty("ANEXO", anexo);
		VO.setProperty("NOME", nome);

		dwfFacade.createEntity("AD_ANEXOSEMAILFLOW", (EntityVO) VO);
		
		salvarArquivo(anexo);
	}

	public byte[] getBytesFromInputStream(InputStream is) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[0xFFFF];
		for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
			os.write(buffer, 0, len);
		}
		return os.toByteArray();
	}
	
	public BigDecimal verificaUltimoID() throws Exception {
		
		BigDecimal count = null;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT NVL(MAX(ID)+1,1) AS ID FROM AD_EMAILFLOW");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getBigDecimal("ID");
		}
		return count;

	}
	
	public void verificaEmail(Part p, BigDecimal id) throws Exception {
		 
		Object content = p.getContent(); 
 
		if (p instanceof Message) {
			// Call methos writeEnvelope
			verificaRemetenteAssunto((Message) p, id);
			verificaAnexos(content,id);
		}
		
		System.out.println("Type: " + p.getContentType());
		String conteudo = getText(p);
		String plainText= Jsoup.parse(conteudo).text();
		
		salvaConteudo(id,plainText);
		
        
	}
	
	public void verificaRemetenteAssunto(Message m, BigDecimal id) throws Exception {
	      
		  Address remetente;
	      String assunto = new String();
	      Date data = m.getSentDate();

	      // FROM
	      if ((remetente = m.getFrom()[0]) != null) {
	         remetente = m.getFrom()[0];
	      }

	      // SUBJECT
	      if (m.getSubject() != null) {
	    	  assunto = m.getSubject();
	      }

	      salvaDados(assunto,remetente,id, data);
	   }
	
	private void verificaAnexos(Object content, BigDecimal id) throws MessagingException, IOException, Exception {
		 if(content instanceof Multipart) {
			 
			 Multipart multipart = (Multipart) content; 
			 
			 for (int k = 0; k < multipart.getCount(); k++) {
		    	 BodyPart bodyPart = multipart.getBodyPart(k);
		    	 
		    	 if(!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) && StringUtils.isBlank(bodyPart.getFileName())) {
		              continue; // dealing with attachments only
		          }
		    	 
		    	 InputStream is = bodyPart.getInputStream();
		    	 byte[] bytesFromInputStream = getBytesFromInputStream(is);
		    	 
		    	 String nome = bodyPart.getFileName();
		    	 
		    	 salvaAnexo(id,new BigDecimal(k),bytesFromInputStream,nome);
		     }
		 }
	}
	
	private void salvaConteudo(BigDecimal id, String cont) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_EMAILFLOW","this.ID=?", new Object[] { id }));
		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;

			VO.setProperty("CONTEUDO", cont);

			itemEntity.setValueObject(NVO);
			
			conteudo ="";
			conteudo = cont;
		}
	}
	
	private String getText(Part p) throws MessagingException, IOException {
		
		boolean textIsHtml = false;
		
		if (p.isMimeType("text/*")) {
			String s = (String) p.getContent();
			textIsHtml = p.isMimeType("text/html");
			return s;
		}

		if (p.isMimeType("multipart/alternative")) {
			Multipart mp = (Multipart) p.getContent();
			String text = null;
			for (int i = 0; i < mp.getCount(); i++) {
				Part bp = mp.getBodyPart(i);
				if (bp.isMimeType("text/plain")) {
					if (text == null)
						text = getText(bp);
					continue;
				} else if (bp.isMimeType("text/html")) {
					String s = getText(bp);
					if (s != null)
						return s;
				} else {
					return getText(bp);
				}
			}
			return text;
		} else if (p.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) p.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				String s = getText(mp.getBodyPart(i));
				if (s != null)
					return s;
			}
		}

		return null;
	}
	
	//Requisi��es HTTP
	public void Post_JSON(String query_url,String request){
		
		try {
			
			URL url = new URL(query_url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			conn.setConnectTimeout(5000);
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			
			OutputStream os = conn.getOutputStream();
			byte[] b = request.getBytes("UTF-8");
			os.write(b);
			os.flush();
			os.close();
			
			InputStream in = new BufferedInputStream(conn.getInputStream());
			byte[] res = new byte[2048];
			int i = 0;
			StringBuilder response = new StringBuilder();
			while ((i = in.read(res)) != -1) {
				response.append(new String(res, 0, i));
			}
			in.close();

			//System.out.println("Response= " + response.toString());
			resp = response.toString();
			
		} catch (Exception e) {
			System.out.println("ERRRO !"+e.getMessage());
		}
	}
	
	public String getJssesionId(String response) {
		String jsessionid = null;
		
		Pattern p = Pattern.compile("<jsessionid>(\\S+)</jsessionid>");
		Matcher m = p.matcher(response);
		if (m.find()) {
			jsessionid = m.group(1);
		}
		
		return jsessionid;
	}
	
	public BigDecimal pegaVersao() throws Exception {
		BigDecimal count = null;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(VERSAO) AS VERSAO FROM TWFPRN WHERE CODPRN="+programa);
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getBigDecimal("VERSAO");
		}
		return count;
	}
	
	//Salva Anexo no reposit�rio de Arquivos
	private void salvarArquivo(byte[] data) throws Exception {
		try {
			
			FileUtils.writeByteArrayToFile(new File(SWRepositoryUtils.getBaseFolder() + PATH_ANEXO + nomeInstancia,chaveMD5), data);
			nomeArquivo="";
			nomeArquivo = chaveMD5;
			
		} catch (Exception e) {
			System.out.println("Nao foi possivel salvar o anexo no repositorio! "+e.getMessage());
		}
		
	}
	
	private String buildChaveArquivo() {
		String a = new Timestamp(System.currentTimeMillis()).toString();
		String b = a.replaceAll("[^a-zZ-Z1-9]","");
		return b;
	}
	
	private void insertAnexoNoFluxoFLow(BigDecimal id) throws Exception {
		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("InstanciaVariavel");
		DynamicVO VO = (DynamicVO) NPVO;
		
		VO.setProperty("IDINSTPRN", pegaIdProcesso(id));
		VO.setProperty("IDINSTTAR", new BigDecimal(0));
		VO.setProperty("NOME", campoQueRecebeAnexoComercial);
		VO.setProperty("TIPO", "A");
		VO.setProperty("TEXTO", "Anexo Comercial - "+id+".xls");
		VO.setProperty("TEXTOLONGO", "{\"name\":\"Anexo Comercial - "+id+".xls\",\"size\":0,\"type\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\",\"path\":\"Repo://Sistema/workflow/formularios/arquivos/"+nomeArquivo+"\",\"lastModifiedDate\":\""+new Timestamp(System.currentTimeMillis()).toString()+"\",\"codUsu\":"+codusu+"}");
		
		dwfFacade.createEntity("InstanciaVariavel", (EntityVO) VO);
	}
	
	private BigDecimal pegaIdProcesso(BigDecimal id) throws Exception {
		BigDecimal count = null;

		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT IDINSTPRN FROM TWFIVAR WHERE NOME='"+campoIdDoEmail+"' AND TEXTO='"+id.toString()+"' AND IDINSTPRN IN (SELECT IDINSTPRN FROM TWFIPRN WHERE DHCONCLUSAO IS NULL AND CODUSUINC="+codusu+")");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getBigDecimal("IDINSTPRN");
		}
		idflow = count;
		return count;
	}
	
	private void enviarEmail(String email, BigDecimal idFlow) throws Exception {
		
		String mensagem = new String();
		
		mensagem = "Prezado,<br/><br/> "
				+ "A sua solicita��o referente ao e-mail \""+assuntoEmail+"\" enviado na data ("+dtEmail+"). "
				+ "<br/><br/>Gerou o fluxo flow n�mero: <b>"+idFlow+"</b>."
				+ "<br/><br/>O processo est� em analise por parte do setor de contratos."
				+ "<br/><br/><b>Este � um e-mail autom�tico por gentileza n�o responder !</b>"
				+ "<br/><br/>Atencionamente,"
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
		VO.setProperty("ASSUNTO", new String("FLOW - "+idFlow));
		VO.setProperty("EMAIL", email);
		VO.setProperty("CODUSU", new BigDecimal(0));
		VO.setProperty("STATUS", "Pendente");
		VO.setProperty("CODCON", new BigDecimal(0));		
		
		dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
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
