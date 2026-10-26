import { expect, test } from "../fixtures";

test("닉네임 저장은 로그인 제공자의 사진을 유지하고 새로 조회한 값을 표시할 것이다", async ({ page, login, request }) => {
  // given
  const session = await login();
  await page.goto("/mypage");
  await expect(page.getByRole("banner").getByRole("img", { name: "테스트 회원 프로필 사진" })).toHaveAttribute("src", "/watchtower.png");
  await page.getByRole("button", { name: "프로필 수정" }).click();
  await expect(page.getByLabel("프로필 사진 URL")).toHaveCount(0);
  await expect(page.getByRole("heading", { name: "변화를 받을 곳" })).toHaveCount(0);
  // when
  await page.getByLabel("닉네임").fill("바뀐 회원");
  await page.getByRole("button", { name: "저장", exact: true }).click();
  // then
  await expect(page.getByRole("alert").filter({ hasText: "프로필을 저장했어요." })).toBeVisible();
  await expect(page.getByLabel("닉네임")).toHaveValue("바뀐 회원");
  const records = await (await request.get(`http://127.0.0.1:4100/__test/records?session=${session}`)).json();
  expect(records.data).toContainEqual(expect.objectContaining({ method: "PUT", path: "/api/v1/member/7", csrf: "csrf-fixture", body: { nickname: "바뀐 회원", profileImageUrl: "/watchtower.png" } }));
});
test("탈퇴하면 서버에서 계정을 삭제하고 홈으로 이동할 것이다", async ({ page, login }) => {
  // given
  await login();
  await page.goto("/mypage");
  await page.getByRole("button", { name: "회원 탈퇴" }).click();
  // when
  await page.getByRole("button", { name: "탈퇴", exact: true }).click();
  // then
  await expect(page).toHaveURL("http://127.0.0.1:3000/");
});
