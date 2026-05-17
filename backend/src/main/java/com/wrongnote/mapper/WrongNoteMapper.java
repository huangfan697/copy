package com.wrongnote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wrongnote.entity.WrongNote;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WrongNoteMapper extends BaseMapper<WrongNote> {
}
