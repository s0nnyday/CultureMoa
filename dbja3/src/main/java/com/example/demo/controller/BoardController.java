package com.example.demo.controller;

import java.util.ArrayList;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.example.demo.dao.BoardDAO_jpa;
import com.example.demo.dao.EventDAO_jpa;
import com.example.demo.entity.Board;
import com.example.demo.entity.Event;
import com.example.demo.entity.Member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Setter;
import java.io.File;

import com.example.demo.dao.ReviewBoardDAO_jpa;
import com.example.demo.entity.Reviewboard;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@Setter
public class BoardController {
	//로그인 했을때만 이용가능 
	//로그인 했을때 아이디
	public String id="user02";
	
	public int pageSIZE = 25;
	public int totalRecord;
	public int totalPage;
	
	@Autowired
	private BoardDAO_jpa boarddao_jpa;
	@Autowired
	private EventDAO_jpa eventdao_jpa;
	@Autowired
	private ReviewBoardDAO_jpa reviewboarddao_jpa;
	
	@GetMapping("/boards/review/reviewDetail")
	public String reviewDetail(@RequestParam int reviewno,Model model) {
		Reviewboard review=new Reviewboard();
		review=reviewboarddao_jpa.findByNo(reviewno);
		int eventno=review.getEventno();
		//게시물 내용 가져오기
		model.addAttribute("r", review);
		//아이디
		model.addAttribute("id", id);
		//공연정보
		Event event=new Event();
		event= eventdao_jpa.findByEventno(eventno);
		model.addAttribute("event", event);
		
		
		String start=event.getEventstart().toString();
		String end=event.getEventend().toString();
		String day=start.substring(0, start.indexOf(" "))+" ~ "+end.substring(0, end.indexOf(" "));
		model.addAttribute("day", day);
		return "/boards/review/reviewDetail";
	}
	
	
	//--------------------------review
	@GetMapping(value={"/boards/review/reviewlist", "/boards/review/reviewlist", "/boards/review/reviewlist/{page}", 
			"/boards/review/reviewlist/{keyword}/{page}", "/boards/review/reviewlist/{keyword}/{page}/{orderby}"})
	public ModelAndView reviewlist(HttpSession session, @PathVariable(required=false) String keyword, 
			@PathVariable(required=false) String orderby, HttpServletRequest request, @PathVariable(required = false) Integer page) {
		ModelAndView mav = new ModelAndView("/boards/review/reviewlist");

		if(orderby == null) {
			if(session.getAttribute("orderby") != null && !session.getAttribute("orderby").equals("")) {
				orderby = (String) session.getAttribute("orderby");
			}
			else {
				orderby="regdate";
			}	
		}
		String key = "all";
		if(page == null) {
			page = 1;
		}
		if(keyword == null) {
			key = "all";
		}

		Page<Reviewboard> list;
		
		if(session.getAttribute("keyword")!=null) {
			key = (String)session.getAttribute("keyword");
		}
		if(keyword != null) {
			key = keyword;
		}
		
		Pageable pageable;
		
		if(key.equals("all")) {
			if(orderby.equals("regdate")) {
				pageable = PageRequest.of(page-1, pageSIZE, Sort.by("regdate").descending());
			}
			else {
			    pageable = PageRequest.of(page-1, pageSIZE, Sort.by("reviewhit").descending());
			}
			list = reviewboarddao_jpa.findByBcategory("", pageable);
		}
		else {
			if(orderby.equals("regdate")) {
			    pageable = PageRequest.of(page-1, pageSIZE, Sort.by("regdate").descending());
			}
			else {
			    pageable = PageRequest.of(page-1, pageSIZE, Sort.by("reviewhit").descending());
			}
			list = reviewboarddao_jpa.findByBcategory(keyword, pageable);

		}
		
	    List<List<Reviewboard>> rows = new ArrayList<>();
	    List<Reviewboard> boardlist = new ArrayList<Reviewboard>();
	    List<Reviewboard> currentRow = null;
		session.setAttribute("keyword", key);
		session.setAttribute("orderby", orderby);
		
	    for (Reviewboard board : list.getContent()) {
	    	
	    	boardlist.add(board);
	        if (currentRow == null || currentRow.size() >= 4) {
	            currentRow = new ArrayList<>();
	            rows.add(currentRow);
	        }
	        currentRow.add(board);
	    }
		
		mav.addObject("list", boardlist);
		mav.addObject("currentPage", page);
		mav.addObject("totalPages", list.getTotalPages());

		return mav;
	}
	
	//후기 게시글 작성 페이지
	@GetMapping("/boards/review/insertBoard_review")
	public void reivew(Model model) {
		model.addAttribute("list", eventdao_jpa.findAll());
		model.addAttribute("id", id);
	}
	
	//후기 게시글 작성
	@PostMapping("/board")
	public ModelAndView board(Reviewboard r,String Contents) {
		ModelAndView mav=new ModelAndView("redirect:/boards/review/reviewlist");
		//게시물 작성하기
		r.setReviewno(reviewboarddao_jpa.findNextNo());
	  
		// Member 객체 생성 및 설정
		Member member = new Member();
		member.setId(id);
		// Reviewboard 객체에 Member 객체 설정
		r.setMember(member);
			
		r.setRegdate(new Date());
		r.setReviewhit(1);
		r.setReviewcontent(Contents);
		reviewboarddao_jpa.save(r);
		return mav;
	}
	
	//후기 게시글을 작성하고 목록으로 돌아가면 폴더에 저장된 사진 삭제
	@PostMapping("/deleteSummernoteImageFile")
	@ResponseBody
	public void deleteSummernoteImageFile(@RequestParam("url") String url) {
		System.out.println("url:"+url);
		File innerFile=new File(url);
		innerFile.delete(); // 내부에 저장된 파일 삭제
		
		String fileRoot = "C:\\summer\\";	//저장될 외부 파일 경로
		String savedFileName=url.substring(url.lastIndexOf("/")+1, url.length());
		System.out.println("savedFileName"+savedFileName);
		File targetFile = new File(fileRoot + savedFileName);	
		FileUtils.deleteQuietly(targetFile);	//저장된 파일 삭제
		
	}
	
	//후기 게시글 작성 시 사진이 있으면 폴더에 저장
	@PostMapping(value="/uploadSummernoteImageFile")
	@ResponseBody
	public String uploadSummernoteImageFile(@RequestParam("file") MultipartFile multipartFile) {
		JsonObject jsonObject = new JsonObject();
		
		String fileRoot = "C:\\summer\\";	//저장될 외부 파일 경로
		String originalFileName = multipartFile.getOriginalFilename();	//오리지날 파일명
		String extension = originalFileName.substring(originalFileName.lastIndexOf("."));	//파일 확장자
				
		String savedFileName = UUID.randomUUID() + extension;	//저장될 파일 명
		File targetFile = new File(fileRoot + savedFileName);	
		System.out.println(targetFile);
		HashMap<String, String> jsonResponse = new HashMap<>();
		
		try {
			InputStream fileStream = multipartFile.getInputStream();
			FileUtils.copyInputStreamToFile(fileStream, targetFile);	//파일 저장
			jsonResponse.put("url", "/summernoteImage/" + savedFileName);
			jsonResponse.put("responseCode", "success");

		} catch (IOException e) {
			FileUtils.deleteQuietly(targetFile);	//저장된 파일 삭제
			jsonResponse.put("responseCode", "error");
			e.printStackTrace();
		}
		 // Gson 객체를 사용하여 HashMap을 JSON 문자열로 변환
	    Gson gson = new Gson();
	    String jsonString = gson.toJson(jsonResponse);
	    
		
		return jsonString;
	}
	
	@GetMapping(value={"/boards/board/freelist", "/boards/board/freelist/", "/boards/board/freelist/{page}", 
			"/boards/board/freelist/{keyword}/{page}", "/boards/board/freelist/{keyword}/{page}/{orderby}"})
	public ModelAndView freelist(HttpSession session, @PathVariable(required=false) String keyword, 
			@PathVariable(required=false) String orderby, HttpServletRequest request, @PathVariable(required = false) Integer page) {
		ModelAndView mav = new ModelAndView("/boards/board/freelist");

		if(orderby == null) {
			if(session.getAttribute("orderby") != null && !session.getAttribute("orderby").equals("")) {
				orderby = (String) session.getAttribute("orderby");
			}
			else {
				orderby="regdate";
			}	
		}
		String key = "all";
		if(page == null) {
			page = 1;
		}
		if(keyword == null) {
			key = "all";
		}

		Page<Board> list;
		
		if(session.getAttribute("keyword")!=null) {
			key = (String)session.getAttribute("keyword");
		}
		if(keyword != null) {
			key = keyword;
		}
		
		Pageable pageable;
		
		if(key.equals("all")) {
			if(orderby.equals("regdate")) {
				pageable = PageRequest.of(page-1, pageSIZE, Sort.by("regdate").descending());
			}
			else {
			    pageable = PageRequest.of(page-1, pageSIZE, Sort.by("boardhit").descending());
			}
			list = boarddao_jpa.findByBcategory("자유", "", pageable);
		}
		else {
			if(orderby.equals("regdate")) {
			    pageable = PageRequest.of(page-1, pageSIZE, Sort.by("regdate").descending());
			}
			else {
			    pageable = PageRequest.of(page-1, pageSIZE, Sort.by("boardhit").descending());
			}
			list = boarddao_jpa.findByBcategory("자유", keyword, pageable);

		}
		
	    List<List<Board>> rows = new ArrayList<>();
	    List<Board> boardlist = new ArrayList<Board>();
	    List<Board> currentRow = null;
		session.setAttribute("keyword", key);
		session.setAttribute("orderby", orderby);
		
	    for (Board board : list.getContent()) {
	    	
	    	boardlist.add(board);
	        if (currentRow == null || currentRow.size() >= 4) {
	            currentRow = new ArrayList<>();
	            rows.add(currentRow);
	        }
	        currentRow.add(board);
	    }
		
		mav.addObject("list", boardlist);
		mav.addObject("currentPage", page);
		mav.addObject("totalPages", list.getTotalPages());

		return mav;
	}
	
	
	@GetMapping("/boards/board/togetherlist")
	public void togetherlist() {
		
	}
	
}
