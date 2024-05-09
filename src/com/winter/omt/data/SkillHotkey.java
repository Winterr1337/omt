package com.winter.omt.data;

public class SkillHotkey {
	public int skillId;

	public SkillHotkey(int skillId) {
		this.skillId = skillId;
	}

	public String toString() {

		return "skillId: " + skillId;

	}

	public boolean equals(SkillHotkey hotkey) {

		return this.skillId == hotkey.skillId;

	}

	public int getSkillId() {

		return skillId;
	}

}
